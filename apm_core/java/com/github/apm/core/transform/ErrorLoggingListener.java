package com.github.apm.core.transform;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

class ErrorLoggingListener extends AgentBuilder.Listener.Adapter {
  @Override
  public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
      Throwable throwable) {
    System.out.println("ERROR on transformation " + typeName + throwable.getMessage());
  }

  @Override
  public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
      JavaModule module, boolean loaded, DynamicType dynamicType) {
    System.out.println("onTransformation:" + typeDescription.getName());
  }

  @Override
  public void onComplete(String arg0, ClassLoader arg1, JavaModule arg2, boolean loaded) {}
}
