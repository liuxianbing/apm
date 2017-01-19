package com.github.apm.core.transform;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.apm.core.util.IOUtils;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

class FileExportingListener extends AgentBuilder.Listener.Adapter {

  private final Collection<String> exportClassesWithName;
  static final List<String> exportedClasses = new ArrayList<String>();

  FileExportingListener(Collection<String> exportClassesWithName) {
    this.exportClassesWithName = exportClassesWithName;
  }

  @Override
  public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
      JavaModule module, DynamicType dynamicType) {
    if (!exportClassesWithName.contains(typeDescription.getName())) {
      return;
    }
    final File exportedClass;
    try {
      exportedClass = File.createTempFile(typeDescription.getName(), ".class");
      IOUtils.copy(new ByteArrayInputStream(dynamicType.getBytes()),
          new FileOutputStream(exportedClass));
      System.out
          .println("Exported class modified by Byte Buddy: {}" + exportedClass.getAbsolutePath());
      exportedClasses.add(exportedClass.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}
