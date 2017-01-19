package com.github.apm.core.transform;

import static com.github.apm.core.transform.ClassLoaderNameMatcher.classLoaderWithName;
import static com.github.apm.core.transform.ClassLoaderNameMatcher.isReflectionClassLoader;
import static com.github.apm.core.transform.TimedElementMatcherDecorator.timed;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isBootstrapClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.apm.core.ApmMonitor;
import com.github.apm.core.configuration.ApmConfiguration;
import com.github.apm.core.dispatcher.Dispatcher;
import com.github.apm.core.util.ClassUtils;
import com.github.apm.core.util.PropertiesUtil;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Ignored;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

/**
 * Attaches the {@link ByteBuddyAgent} at runtime and registers all
 * {@link StagemonitorByteBuddyTransformer}s
 */
public class AgentAttacher {

  private static final String IGNORED_CLASSLOADERS_KEY =
      AgentAttacher.class.getName() + "hashCodesOfClassLoadersToIgnore";
  private static final Runnable NOOP_ON_SHUTDOWN_ACTION = new Runnable() {
    public void run() {}
  };

  public static volatile boolean runtimeAttached = false;
  private static Set<String> hashCodesOfClassLoadersToIgnore = Collections.emptySet();
  public static Instrumentation instrumentation;
  private final static List<ClassFileTransformer> classFileTransformers =
      new ArrayList<ClassFileTransformer>();
  private final static AutoEvictingCachingBinaryLocator binaryLocator =
      new AutoEvictingCachingBinaryLocator();

  private AgentAttacher() {}

  /**
   * 再次切入
   */
  public static synchronized void performRuntimeAttachmentAgain() {
    final long start = System.currentTimeMillis();
    classFileTransformers.add(initByteBuddyClassFileTransformer(binaryLocator));
    if (ApmConfiguration.getInstance().debug) {
      System.out.println(
          "performRuntimeAttachmentAgain agents in {} ms" + (System.currentTimeMillis() - start));
    }
    TimedElementMatcherDecorator.logMetrics();
  }

  /**
   * Attaches the profiler and other instrumenters at runtime so that it is not necessary to add the
   * -javaagent command line argument.
   *
   * @return A runnable that should be called on shutdown to unregister this class file transformer
   */
  public static synchronized Runnable performRuntimeAttachment() {
    if (runtimeAttached || !ApmConfiguration.getInstance().active
        || !ApmConfiguration.getInstance().runtimeAttach) {
      return NOOP_ON_SHUTDOWN_ACTION;
    }
    runtimeAttached = true;

    if (initInstrumentation()) {
      final long start = System.currentTimeMillis();
      classFileTransformers.add(initByteBuddyClassFileTransformer(binaryLocator));
      if (ApmConfiguration.getInstance().debug) {
        System.out.println("Attached agents in {} ms" + (System.currentTimeMillis() - start));
      }
      TimedElementMatcherDecorator.logMetrics();
    }
    return new Runnable() {
      public void run() {
        for (ClassFileTransformer classFileTransformer : classFileTransformers) {
          instrumentation.removeTransformer(classFileTransformer);
        }
        // This ClassLoader is shutting down so don't try to retransform classes of it in the future
        hashCodesOfClassLoadersToIgnore
            .add(ClassUtils.getIdentityString(AgentAttacher.class.getClassLoader()));
        binaryLocator.close();
      }
    };
  }

  private static boolean initInstrumentation() {
    if (instrumentation == null) {
      try {
        instrumentation = ByteBuddyAgent.getInstrumentation();
      } catch (IllegalStateException e) {
        e.printStackTrace();
        instrumentation = ByteBuddyAgent.install(new ByteBuddyAgent.AttachmentProvider.Compound(
            ByteBuddyAgent.AttachmentProvider.DEFAULT));
        System.out.println(e.getMessage() + "--initInstrumentation--");
      }
    }
    try {
      ensureDispatcherIsAppendedToBootstrapClasspath(instrumentation);
      if (!Dispatcher.getValues().containsKey(IGNORED_CLASSLOADERS_KEY)) {
        Dispatcher.put(IGNORED_CLASSLOADERS_KEY,
            Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>()));
      }
      hashCodesOfClassLoadersToIgnore = Dispatcher.get(IGNORED_CLASSLOADERS_KEY);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(
          "Failed to perform runtime attachment of the stagemonitor agent. Make sure that you run your "
              + "application with a JDK (not a JRE)."
              + "To make apmmonitor work with a JRE, you have to add the following command line argument to the "
              + "start of the JVM: -javaagent:/path/to/byte-buddy-agent-<version>.jar" + e);
      return false;
    }
  }


  private static void ensureDispatcherIsAppendedToBootstrapClasspath(
      Instrumentation instrumentation) throws ClassNotFoundException, IOException {
    // final ClassLoader bootstrapClassloader = ClassLoader.getSystemClassLoader().getParent();
    // try {
    // bootstrapClassloader.loadClass(DISPATCHER_CLASS_NAME);
    // // already injected
    // } catch (ClassNotFoundException e) {
    // final JarFile jarfile = new JarFile(createTempDispatcherJar());
    // instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
    // bootstrapClassloader.loadClass(DISPATCHER_CLASS_NAME);
    // }
  }

  // private static File createTempDispatcherJar() throws IOException {
  // final InputStream input = AgentAttacher.class.getClassLoader()
  // .getResourceAsStream("stagemonitor-dispatcher.jar.gradlePleaseDontExtract");
  // if (input == null) {
  // throw new IllegalStateException(
  // "If you see this exception inside you IDE, you have to execute gradle "
  // + "processResources before running the tests.");
  // }
  // final File tempDispatcherJar = File.createTempFile("stagemonitor-dispatcher", ".jar");
  // tempDispatcherJar.deleteOnExit();
  // IOUtils.copy(input, new FileOutputStream(tempDispatcherJar));
  // return tempDispatcherJar;
  // }

  private static ClassFileTransformer initByteBuddyClassFileTransformer(
      AutoEvictingCachingBinaryLocator binaryLocator) {
    AgentBuilder agentBuilder = createAgentBuilder(binaryLocator);
    for (ApmMonitorByteBuddyTransformer transformer : getApmMonitorByteBuddyTransformers()) {
      if (null == Dispatcher.get(transformer.getClass().getName() + "_Transformers")) {
        agentBuilder = agentBuilder.type(transformer.getMatcher())
            .transform(transformer.getTransformer()).asDecorator();
        Dispatcher.put(transformer.getClass().getName() + "_Transformers", true);
        System.out.println("initByteBuddyClassFileTransformer:" + transformer.getClass().getName());
      }
    }

    final long start = System.currentTimeMillis();
    try {
      return agentBuilder.installOn(instrumentation);
    } finally {
      if (ApmConfiguration.getInstance().debug) {
        System.out.println("Installed agent in {} ms" + (System.currentTimeMillis() - start));
      }
    }
  }

  private static AgentBuilder createAgentBuilder2(AutoEvictingCachingBinaryLocator binaryLocator) {
    final ByteBuddy byteBuddy =
        new ByteBuddy().with(TypeValidation.of(ApmConfiguration.getInstance().debug));
    // .with(MethodGraph.Empty.INSTANCE);
    return new AgentBuilder.Default(byteBuddy).with(binaryLocator)
        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION).with(new AgentBuilder.Listener() {
          @Override
          public void onComplete(String arg0, ClassLoader arg1, JavaModule arg2) {
            // System.out.println("onComplete:" + arg0);
          }

          @Override
          public void onError(String arg0, ClassLoader arg1, JavaModule arg2, Throwable arg3) {
            System.out.println(arg3.getMessage() + "onError:" + arg0);
            arg3.printStackTrace();
          }

          @Override
          public void onIgnored(TypeDescription arg0, ClassLoader arg1, JavaModule arg2) {}

          @Override
          public void onTransformation(TypeDescription arg0, ClassLoader arg1, JavaModule arg2,
              DynamicType arg3) {
            System.out.println("onTransformation" + arg0.getName());
          }
        }).ignore(any(), timed("classloader", "bootstrap", isBootstrapClassLoader()))
        .or(any(), timed("classloader", "reflection", isReflectionClassLoader()))
        .or(any(),
            timed("classloader", "apm-call-site",
                classLoaderWithName("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader")))
        .or(any(), new IsIgnoredClassLoaderElementMatcher())
        // .or(timed("type", "global-exclude",
        // nameStartsWith("java").or(nameStartsWith("com.sun.")).or(nameStartsWith("sun."))
        // .or(nameStartsWith("jdk.")).or(nameStartsWith("org.aspectj."))
        // .or(nameStartsWith("org.groovy.")).or(nameStartsWith("com.p6spy."))
        // .or(nameStartsWith("net.bytebuddy."))
        // .or(nameStartsWith("org.slf4j.").and(not(nameStartsWith("org.slf4j.impl."))))
        // .or(nameContains("javassist")).or(nameContains(".asm."))
        // .or(nameStartsWith("com.github.apm")
        // .and(not(nameContains("Test").or(nameContains("benchmark")))))))
        .disableClassFormatChanges().type(ElementMatchers.nameStartsWith("com.jingoal.util"))
        .transform(new AgentBuilder.Transformer() {
          @Override
          public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription,
              ClassLoader arg2) {
            try {
              // return builder.visit(Advice.to(LoggingAdvice.class).on(nameStartsWith("test")));
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        }).asDecorator();
  }

  private static AgentBuilder createAgentBuilder(AutoEvictingCachingBinaryLocator binaryLocator) {
    final ByteBuddy byteBuddy =
        new ByteBuddy().with(TypeValidation.of(ApmConfiguration.getInstance().debug));
    // .with(MethodGraph.Empty.INSTANCE);

    // AgentBuilder defaultBuilder
    Ignored ignored = new AgentBuilder.Default(byteBuddy)
        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION).with(getListener())
        .with(binaryLocator)
        .ignore(any(), timed("classloader", "bootstrap", isBootstrapClassLoader()))
        .or(any(), timed("classloader", "reflection", isReflectionClassLoader()))
        .or(any(),
            timed("classloader", "apm-call-site",
                classLoaderWithName("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader")))
        .or(any(), new IsIgnoredClassLoaderElementMatcher())
        .or(timed("type", "global-exclude",
            nameStartsWith("java").or(nameStartsWith("com.sun.")).or(nameStartsWith("sun."))
                .or(nameStartsWith("jdk.")).or(nameStartsWith("org.aspectj."))
                .or(nameStartsWith("org.groovy.")).or(nameStartsWith("com.p6spy."))
                .or(nameStartsWith("net.bytebuddy."))
                .or(nameStartsWith("org.slf4j.").and(not(nameStartsWith("org.slf4j.impl."))))
                .or(nameContains("javassist")).or(nameContains(".asm."))
                .or(nameStartsWith("com.github.apm")
                    .and(not(nameContains("Test").or(nameContains("benchmark")))))));
    Map<String, String> map = PropertiesUtil.getFromKeyByPrefix("apm.instrument.agentBuilderIgnore",
        ApmConfiguration.getInstance().propertiesMap);
    if (map.size() > 0) {
      Iterator<Entry<String, String>> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Entry<String, String> en = it.next();
        ElementMatcher.Junction<TypeDescription> matcher = none();
        String[] tmp = en.getValue().split(",");
        if (en.getKey().endsWith("nameContains")) {
          for (String cla : tmp)
            matcher = matcher.or(nameContains(cla));
          ignored = ignored.or(matcher);
        } else if (en.getKey().endsWith("nameStartsWith")) {
          for (String cla : tmp)
            matcher = matcher.or(nameStartsWith(cla));
          ignored = ignored.or(matcher);
        }
      }
    }
    AgentBuilder defaultBuilder = ignored.disableClassFormatChanges();
    return defaultBuilder;
  }

  private static AgentBuilder.Listener getListener() {
    List<AgentBuilder.Listener> listeners = new ArrayList<AgentBuilder.Listener>(2);
    if (ApmConfiguration.getInstance().debug) {
      listeners.add(new ErrorLoggingListener());
    }
    if (ApmConfiguration.getInstance().exportGeneratedClassesWithName.length() > 0) {
      listeners.add(new FileExportingListener(
          Arrays.asList(ApmConfiguration.getInstance().exportGeneratedClassesWithName.split(","))));
    }
    return new AgentBuilder.Listener.Compound(listeners);
  }

  private static Iterable<ApmMonitorByteBuddyTransformer> getApmMonitorByteBuddyTransformers() {
    List<ApmMonitorByteBuddyTransformer> transformers =
        new ArrayList<ApmMonitorByteBuddyTransformer>();
    for (ApmMonitorByteBuddyTransformer transformer : ServiceLoader
        .load(ApmMonitorByteBuddyTransformer.class, ApmMonitor.class.getClassLoader())) {
      System.out.println("transformer is:" + transformer.getClass().getName());
      if (transformer.isActive() && !isExcluded(transformer)) {
        transformers.add(transformer);
        if (ApmConfiguration.getInstance().debug) {
          System.out.println("Registering {}" + transformer.getClass().getSimpleName());
        }
      } else if (ApmConfiguration.getInstance().debug) {
        System.out.println("Excluding {}" + transformer.getClass().getSimpleName());
      }
    }
    Collections.sort(transformers, new Comparator<ApmMonitorByteBuddyTransformer>() {
      @Override
      public int compare(ApmMonitorByteBuddyTransformer o1, ApmMonitorByteBuddyTransformer o2) {
        return o1.getOrder() > o2.getOrder() ? -1 : 1;
      }
    });
    return transformers;
  }

  private static boolean isExcluded(ApmMonitorByteBuddyTransformer transformer) {
    if (ApmConfiguration.getInstance().excludedInstrumenter.length() == 0) {
      return false;
    }
    return Arrays.asList(ApmConfiguration.getInstance().excludedInstrumenter.split(","))
        .contains(transformer.getClass().getSimpleName());
  }

  private static class IsIgnoredClassLoaderElementMatcher implements ElementMatcher<ClassLoader> {
    @Override
    public boolean matches(ClassLoader target) {
      return hashCodesOfClassLoadersToIgnore.contains(ClassUtils.getIdentityString(target));
    }
  }

}
