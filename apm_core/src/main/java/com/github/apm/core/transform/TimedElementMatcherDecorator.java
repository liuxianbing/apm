package com.github.apm.core.transform;


import com.github.apm.core.configuration.ApmConfiguration;

import net.bytebuddy.matcher.ElementMatcher;


public class TimedElementMatcherDecorator<T> implements ElementMatcher<T> {


  private final ElementMatcher<T> delegate;

  private static int count = 0;
  private static long time = 0l;

  public static <T> ElementMatcher<T> timed(String type, String transformerName,
      ElementMatcher<T> delegate) {
    if (ApmConfiguration.getInstance().debug) {
      return new TimedElementMatcherDecorator<T>(delegate, type, transformerName);
    } else {
      return delegate;
    }
  }

  private TimedElementMatcherDecorator(ElementMatcher<T> delegate, String type,
      String transformerName) {
    this.delegate = delegate;
  }

  @Override
  public boolean matches(T target) {
    long start = System.currentTimeMillis();
    try {
      return delegate.matches(target);
    } finally {
      count++;
      time += (System.currentTimeMillis() - start);
    }
  }

  public static void logMetrics() {
    if (ApmConfiguration.getInstance().debug) {
      System.out.println("ElementMatcher TIME (seconds total)");
      System.out.println(count + "Total time: {} ms" + time);
    }
  }
}
