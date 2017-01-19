package com.github.apm.core.transform;

import static com.github.apm.core.transform.ApmmonitorClassNameMatcher.isInsideMonitoredProject;
import static com.github.apm.core.transform.CachedClassLoaderMatcher.cached;
import static com.github.apm.core.transform.TimedElementMatcherDecorator.timed;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;

import java.lang.annotation.Annotation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

public abstract class ApmMonitorByteBuddyTransformer {

  private static final ElementMatcher.Junction<ClassLoader> applicationClassLoaderMatcher =
      cached(new ApplicationClassLoaderMatcher());

  protected final String transformerName = getClass().getSimpleName();

  /**
   * Returns the order of this transformer when multiple transformers match a method.
   * </p>
   * Higher orders will be applied first
   *
   * @return the order
   */
  protected int getOrder() {
    return 0;
  }

  public final AgentBuilder.RawMatcher getMatcher() {
    return new AgentBuilder.RawMatcher() {
      @Override
      public boolean matches(TypeDescription typeDescription, ClassLoader classLoader,
          JavaModule javaModule, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
        // final boolean matches =
        // timed("type", transformerName, getTypeMatcher()).matches(typeDescription)
        // && getRawMatcher().matches(typeDescription, classLoader, javaModule,
        // classBeingRedefined, protectionDomain)
        // && timed("classloader", "application", getClassLoaderMatcher())
        // .matches(classLoader);
        boolean a1 = timed("type", transformerName, getTypeMatcher()).matches(typeDescription);
        boolean a2 = getRawMatcher().matches(typeDescription, classLoader, javaModule,
            classBeingRedefined, protectionDomain);
        boolean a3 =
            timed("classloader", "application", getClassLoaderMatcher()).matches(classLoader);
        System.out.println("a1:" + a1 + ",a2:" + a2 + ",a3:" + a3);
        boolean matches = a1 && a2 && a3;
        System.out.println(classBeingRedefined + "classLoader:" + classLoader);
        System.out.println(typeDescription + "typeDescription:" + matches);
        if (!matches) {
          onIgnored(typeDescription, classLoader);
        }
        return matches;
      }
    };
  }

  protected ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
    return applicationClassLoaderMatcher;
  }

  protected AgentBuilder.RawMatcher getRawMatcher() {
    return NoOpRawMatcher.INSTANCE;
  }

  public void beforeTransformation(TypeDescription typeDescription, ClassLoader classLoader) {}

  protected ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
    return none();
  }

  protected ElementMatcher.Junction<TypeDescription> getNarrowTypesMatcher() {
    return any();
  }

  protected ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
    return none();
  }

  protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
    return isInsideMonitoredProject().or(getExtraIncludeTypeMatcher()).and(getNarrowTypesMatcher());
  }

  protected ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
    return getIncludeTypeMatcher().and(not(isInterface())).and(not(isSynthetic()))
        .and(not(getExtraExcludeTypeMatcher()));
  }

  public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader) {
    if (getTypeMatcher().matches(typeDescription)) {

    }
  }

  public boolean isActive() {
    return true;
  }

  public static abstract class ApmmonitorDynamicValue<T extends Annotation>
      extends Advice.DynamicValue.ForFixedValue<T> {
    public abstract Class<T> getAnnotationClass();
  }

  private Advice.WithCustomMapping registerDynamicValues() {
    List<ApmmonitorDynamicValue<?>> dynamicValues = getDynamicValues();
    Advice.WithCustomMapping withCustomMapping = Advice.withCustomMapping();
    for (ApmmonitorDynamicValue dynamicValue : dynamicValues) {
      withCustomMapping = withCustomMapping.bind(dynamicValue.getAnnotationClass(), dynamicValue);
    }
    return withCustomMapping;
  }

  protected Class<? extends ApmMonitorByteBuddyTransformer> getAdviceClass() {
    return getClass();
  }

  protected List<ApmmonitorDynamicValue<?>> getDynamicValues() {
    return Collections.emptyList();
  }

  public AgentBuilder.Transformer getTransformer() {
    return new AgentBuilder.Transformer() {

      private AsmVisitorWrapper.ForDeclaredMethods advice =
          // registerDynamicValues()
          Advice.to(getAdviceClass())
              .on(timed("method", transformerName, getMethodElementMatcher()));

      @Override
      public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
          TypeDescription typeDescription, ClassLoader classLoader) {
        beforeTransformation(typeDescription, classLoader);
        return builder.visit(advice);
      }

    };
  }

  private static class NoOpRawMatcher implements AgentBuilder.RawMatcher {
    public static final NoOpRawMatcher INSTANCE = new NoOpRawMatcher();

    @Override
    public boolean matches(TypeDescription typeDescription, ClassLoader classLoader,
        JavaModule javaModule, Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
      return true;
    }
  }

  protected ElementMatcher.Junction<MethodDescription> getMethodElementMatcher() {
    return not(isConstructor()).and(not(isAbstract())).and(not(isNative())).and(not(isFinal()))
        .and(not(isSynthetic())).and(not(isTypeInitializer())).and(getExtraMethodElementMatcher());
  }

  protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
    return any();
  }

}
