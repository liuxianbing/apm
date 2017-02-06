package com.github.apm.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class LoadJarUtil {

  private static final Map<String, Long> JAR = new HashMap<>();


  public static void loadJarFolder(String path, final Instrumentation inst) {
    File f = new File(path);
    File[] list = f.listFiles();
    for (File ff : list) {
      if (ff.getName().endsWith(".jar")) {
        loadJar(ff, inst);
      }
    }
  }


  /**
   * 加载jar包
   * 
   * @param path
   * @param inst
   */
  public static void loadJar(File f, final Instrumentation inst) {
    long lastModify = f.lastModified();
    String path = f.getAbsolutePath();
    if ((JAR.get(path) != null && JAR.get(path) < lastModify) || JAR.get(path) == null) {// 已经加载过
                                                                                         // jar包有更新
      try {
        // inst.appendToSystemClassLoaderSearch(new JarFile(path));
        inst.appendToSystemClassLoaderSearch(new JarFile(path));
      } catch (IOException e) {
        e.printStackTrace();
      }
      JAR.put(path, lastModify);
    }
  }

  /**
   * 加载jar包
   * 
   * @param path
   * @param inst
   */
  public static void loadJar(String path, final Instrumentation inst) {
    File f = new File(path);
    System.out.println(f.isDirectory() + "loadJar path:" + path);
    if (f.isDirectory()) {
      loadJarFolder(path, inst);
      return;
    }
    long lastModify = f.lastModified();
    if ((JAR.get(path) != null && JAR.get(path) < lastModify) || JAR.get(path) == null) {// 已经加载过
                                                                                         // jar包有更新
      try {
        inst.appendToSystemClassLoaderSearch(new JarFile(path));
      } catch (IOException e) {
        e.printStackTrace();
      }
      JAR.put(path, lastModify);
    }
  }
}
