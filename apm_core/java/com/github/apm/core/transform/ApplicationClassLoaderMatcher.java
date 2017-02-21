package com.github.apm.core.transform;


import com.github.apm.core.ApmMonitor;
import com.github.apm.core.configuration.ApmConfiguration;
import com.github.apm.core.util.ClassUtils;

import net.bytebuddy.matcher.ElementMatcher;

/**
 * Only allows transformation of classes if the target {@link ClassLoader} is able to load
 * {@link Stagemonitor}.
 * <p/>
 * This avoids ClassNotFoundExceptions that can happen when instrumenting classes whose class
 * loaders don't have access to stagemonitor classes, for example the Profiler class.
 * <p/>
 * Also, this prevents to transform classes that are loaded by another class loader hierarchy and
 * thus avoids interfering with other applications which are deployed on the same application
 * server.
 */
public class ApplicationClassLoaderMatcher
    extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

  @Override
  public boolean matches(ClassLoader target) {
    // only returns true if this class was loaded by the provided classLoader or by a parent of it
    // i.e. only if it is from the same application
    final boolean result = ClassUtils.loadClassOrReturnNull(target,
        "com.github.apm.core.ApmMonitor") == ApmMonitor.class;
    if (ApmConfiguration.getInstance().debug) {
      System.out.println("ApplicationClassLoaderMatcher:" + target + "  result is " + result);
    }
    return result;
  }
}
