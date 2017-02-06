package com.github.apm.core.test;

import net.bytebuddy.asm.Advice;

public class LoggingAdvice {

  @Advice.OnMethodExit(onThrowable = Throwable.class)
  public static void exit(@Advice.Origin("#m") String methodName,
      @Advice.Origin("#t") String classType, @Advice.Origin("#s") String signature) {
    System.out.println(classType + "-LoggingAdvice-:" + methodName + signature);
  }
}
