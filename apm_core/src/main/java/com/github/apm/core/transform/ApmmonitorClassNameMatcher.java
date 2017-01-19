package com.github.apm.core.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.github.apm.core.configuration.ApmConfiguration;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * An {@link ElementMatcher} with the following logic:
 * <ul>
 * <li>Exclude all types which contain <code>apm.instrument.excludeContaining</code></li>
 * <li>Include all types <code>apm.instrument.include</code></li>
 * <li>If there are no more specific excludes in <code>stagemonitor.instrument.exclude</code></li>
 * </ul>
 */
public class ApmmonitorClassNameMatcher
    extends ElementMatcher.Junction.AbstractBase<TypeDescription> {

  private static Collection<String> includes;

  private static Collection<String> excludes;

  private static Collection<String> excludeContaining;

  public static final ApmmonitorClassNameMatcher INSTANCE = new ApmmonitorClassNameMatcher();

  public static ElementMatcher.Junction<TypeDescription> isInsideMonitoredProject() {
    return INSTANCE;
  }

  private ApmmonitorClassNameMatcher() {}

  static {
    initIncludesAndExcludes();
  }

  private static void initIncludesAndExcludes() {

    excludeContaining = Arrays.asList(ApmConfiguration.getInstance().globalExclude.split(","));

    excludes = new ArrayList<String>();
    excludes = Arrays.asList(ApmConfiguration.getInstance().exclude.split(","));
    includes = new ArrayList<String>();
    if (ApmConfiguration.getInstance().include.length() > 0) {
      includes = Arrays.asList(ApmConfiguration.getInstance().include.split(","));
    }
    if (includes.isEmpty()) {
      System.out.println(
          "No includes for instrumentation configured. Please set the stagemonitor.instrument.include property.");
    }
  }

  /**
   * Checks if a specific class should be instrumented with this instrumenter
   * <p/>
   * The default implementation considers the following properties:
   * <ul>
   * <li><code>apm.instrument.globalExclude</code></li>
   * <li><code>apm.instrument.include</code></li>
   * <li><code>apm.instrument.exclude</code></li>
   * </ul>
   *
   * @param className The name of the class. For example java/lang/String
   * @return <code>true</code>, if the class should be instrumented, <code>false</code> otherwise
   */
  public static boolean isIncluded(String className) {
    for (String exclude : excludeContaining) {
      if (className.contains(exclude)) {
        return false;
      }
    }

    for (String include : includes) {
      if (className.startsWith(include)) {
        return !hasMoreSpecificExclude(className, include);
      }
    }
    return false;
  }

  private static boolean hasMoreSpecificExclude(String className, String include) {
    for (String exclude : excludes) {
      if (exclude.length() > include.length() && exclude.startsWith(include)
          && className.startsWith(exclude)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean matches(TypeDescription target) {
    return isIncluded(target.getName());
  }
}
