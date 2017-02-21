package com.github.apm.core.transform.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.apm.core.prometheus.PrometheusMetricsModule;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription.Loadable;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription.InDefinedShape;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * 统计方法执行时间 超时时间
 * 
 * @author liuyang
 *
 */
public class DefaultApmTransformer extends ApmTransformerBasic {

  public DefaultApmTransformer() {
    super();
  }

  @Advice.OnMethodEnter
  public static long enter() {
    return System.currentTimeMillis();
  }

  @Advice.OnMethodExit(onThrowable = Throwable.class)
  public static void exit(@Advice.Thrown Throwable t, @Advice.Origin("#m") String methodName,
      @Advice.Origin("#t") String classType, @Advice.Origin("#s") String signature,
      @Advice.Enter long startTime,
      @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnObj) {
    long spendTime = System.currentTimeMillis() - startTime;
    PrometheusMetricsModule.calLate(spendTime, classType, methodName, signature);
    if (null != t) {
      PrometheusMetricsModule.errorCounter(classType, methodName, signature, t);
    }
    if (slowTime > 0 && spendTime >= slowTime) {
      writeSlowTime(classType, methodName, signature, returnObj.toString(), spendTime);
    }
  }


  @Override
  protected int getOrder() {
    return Integer.MAX_VALUE;
  }

  @Override
  protected List<ApmmonitorDynamicValue<?>> getDynamicValues() {
    return Collections.<ApmmonitorDynamicValue<?>>singletonList(new ProfilerDynamicValue());
  }

  @Override
  protected ElementMatcher.Junction<TypeDescription> getExtraIncludeTypeMatcher() {
    ElementMatcher.Junction<TypeDescription> res =
        generatedTypeMatcher("extra.include.type.matcher");
    return res == null ? super.getExtraIncludeTypeMatcher() : res;
  }

  @Override
  protected ElementMatcher.Junction<TypeDescription> getExtraExcludeTypeMatcher() {
    ElementMatcher.Junction<TypeDescription> res =
        generatedTypeMatcher("extra.exclude.type.matcher");
    return res == null ? super.getExtraExcludeTypeMatcher() : res;
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> getTypeMatcher() {
    ElementMatcher.Junction<TypeDescription> res = generatedTypeMatcher("type.matcher");
    return res == null ? super.getTypeMatcher() : res;
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
    ElementMatcher.Junction<TypeDescription> res = generatedTypeMatcher("include.type.matcher");
    return res == null ? super.getIncludeTypeMatcher() : res;
  }

  @Override
  public ElementMatcher.Junction<MethodDescription> getExtraMethodElementMatcher() {
    ElementMatcher.Junction<MethodDescription> res =
        generateMethodElementMatcher("extra.method.matcher");
    return res == null ? super.getExtraMethodElementMatcher() : res;
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> getClassLoaderMatcher() {
    return generateClassLoaderMatcher();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface ProfilerSignature {
  }

  static class ProfilerDynamicValue extends ApmmonitorDynamicValue<ProfilerSignature> {

    @Override
    public Class<ProfilerSignature> getAnnotationClass() {
      return ProfilerSignature.class;
    }


    public String getSignature(MethodDescription instrumentedMethod) {
      StringBuilder stringBuilder = new StringBuilder();
      boolean comma = false;
      for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList()
          .asErasures()) {
        if (comma) {
          stringBuilder.append(',');
        } else {
          comma = true;
        }
        stringBuilder.append(typeDescription.getSimpleName());
      }
      return stringBuilder.toString();
    }

    @Override
    protected Object doResolve(TypeDescription instrumentedType,
        MethodDescription instrumentedMethod, InDefinedShape target,
        Loadable<ProfilerSignature> annotation, Assigner assigner, boolean initialized) {
      String returnType = instrumentedMethod.getReturnType().asErasure().getSimpleName();
      String className = instrumentedMethod.getDeclaringType().getTypeName();

      List<String> obj = new ArrayList<String>(4);
      obj.add(className);
      obj.add(returnType);
      return obj;

    }
  }

}
