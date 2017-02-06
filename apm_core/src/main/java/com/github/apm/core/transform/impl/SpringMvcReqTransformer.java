package com.github.apm.core.transform.impl;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;

import com.github.apm.core.prometheus.PrometheusMetricsModule;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SpringMvcReqTransformer extends ApmTransformerBasic {

  public SpringMvcReqTransformer() {
    super();
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
    return named("org.springframework.web.servlet.DispatcherServlet");
  }

  @Override
  protected ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
    return named("getHandler")
        .and(returns(named("org.springframework.web.servlet.HandlerExecutionChain")));
  }

  @Advice.OnMethodEnter
  public static long enter() {
    return System.currentTimeMillis();
  }


  @Advice.OnMethodExit
  public static void afterGetHandler(@Advice.Enter long startTime, @Advice.Return Object handler) {
    long spendTime = System.currentTimeMillis() - startTime;
    // System.out.println(handler.getClass().getSimpleName() + "##");
    final HandlerExecutionChain handlerExecutionChain = (HandlerExecutionChain) handler;
    Object obs = handlerExecutionChain.getHandler();
    if (handler != null && obs instanceof HandlerMethod) {
      HandlerMethod handlerMethod = (HandlerMethod) obs;
      String[] obj = new String[3];
      obj[0] = handlerMethod.getBeanType().getSimpleName();
      obj[1] = handlerMethod.getMethod().getName();
      obj[2] = "";
      PrometheusMetricsModule.calLate(spendTime, obj);
    }
    // String[] obj = SpringMvcReqTransformer.getRequestNameFromHandler(handler);
    // if (null != obj) {
    // PrometheusMetricsModule.calLate(spendTime, obj);
    // if (null != t) {
    // PrometheusMetricsModule.errorCounter(obj[0], obj[1], "", t);
    // }
    // if (slowTime > 0 && spendTime >= slowTime) {
    // writeSlowTime(obj[0], obj[1], "", "", spendTime);
    // }
    // }

  }

  private static String[] getRequestNameFromHandler(Object handler) {
    final HandlerExecutionChain handlerExecutionChain = (HandlerExecutionChain) handler;
    if (handler != null && handlerExecutionChain.getHandler() instanceof HandlerMethod) {
      HandlerMethod handlerMethod = (HandlerMethod) handlerExecutionChain.getHandler();
      String[] obj = new String[2];
      obj[0] = handlerMethod.getBeanType().getSimpleName();
      obj[1] = handlerMethod.getMethod().getName();
      return obj;
    }
    return null;
  }
}
