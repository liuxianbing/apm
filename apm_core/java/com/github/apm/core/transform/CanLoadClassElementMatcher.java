package com.github.apm.core.transform;

import com.github.apm.core.util.ClassUtils;

import net.bytebuddy.matcher.ElementMatcher;

/**
 * Matches those {@link ClassLoader}s which are able to load a particular class
 */
public class CanLoadClassElementMatcher implements ElementMatcher<ClassLoader> {

  private final String className;

  public static ElementMatcher<ClassLoader> canLoadClass(String className) {
    return new CanLoadClassElementMatcher(className);
  }

  private CanLoadClassElementMatcher(String className) {
    this.className = className;
  }

  @Override
  public boolean matches(ClassLoader target) {
    return ClassUtils.canLoadClass(target, className);
  }
}
