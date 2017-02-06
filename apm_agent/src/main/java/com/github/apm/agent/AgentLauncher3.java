package com.github.apm.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;

/**
 * Agent切入主类
 * 
 * @author liuyang
 *
 */
public class AgentLauncher3 {

  public static void premain(String args, Instrumentation inst) {
    // main(args, inst);
    new Thread(new InnerThread2(args, inst)).start();
  }

  public static void agentmain(String args, Instrumentation inst) {
    main(args, inst);
  }

  private static synchronized void main1(final String args, final Instrumentation inst) {
    // try {
    // inst.appendToBootstrapClassLoaderSearch(new JarFile(
    // AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile()));
    // } catch (IOException e1) {
    // // TODO Auto-generated catch block
    // e1.printStackTrace();
    // }
    System.out.println("inject.....");

    new Thread(new InnerThread(inst)).start();
    // Class<?>[] classes = inst.getAllLoadedClasses();
    // inst.addTransformer(new InjectMonitorClassTransform(), true);
    // for (Class c : classes) {
    // if (c.getName().contains("com.jingoal.dc.koala.web.controller.LoginController")) {
    // try {
    // System.out.println("trrrrr" + c.getName());
    // // inst.retransformClasses(c);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
  }

  static class InnerThread2 implements Runnable {
    Instrumentation inst;
    String args;

    InnerThread2(String args, Instrumentation inst) {
      this.inst = inst;
      this.args = args;
    }

    public void run() {
      try {
        Thread.currentThread().sleep(18000);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      main(args, inst);
    }
  }

  static class InnerThread implements Runnable {
    Instrumentation inst;

    InnerThread(Instrumentation inst) {
      this.inst = inst;
    }

    public void run() {
      try {
        Thread.currentThread().sleep(18000);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
      Class[] classes = inst.getAllLoadedClasses();
      inst.addTransformer(new InjectMonitorClassTransform(), true);
      for (Class c : classes) {
        if (c.getName().contains("com.jingoal.dc.koala.web.controller.LoginController")) {
          System.out.println("trrrrr" + c.getName());
          try {
            inst.retransformClasses(c);
          } catch (UnmodifiableClassException e) {
            e.printStackTrace();
          }
        }
      }
    }

  }

  private static synchronized void main(final String args, final Instrumentation inst) {
    ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
    // try {
    // Thread.currentThread().sleep(10000);
    // } catch (InterruptedException e2) {
    // // TODO Auto-generated catch block
    // e2.printStackTrace();
    // }
    URLClassLoader webapp = null;
    for (Class a : inst.getAllLoadedClasses()) {
      if (a.getName().contains("LoginController")
          || a.getName().contains("com.jingoal.dc.koala.web.util.TimeUtil")) {
        webapp = (URLClassLoader) a.getClassLoader();
        break;
      }
    }
    // try {
    // Class a = webapp.loadClass("com.jingoal.dc.koala.web.controller.LoginController");
    // System.out.println("LoginController@@--" + a.getClassLoader());
    // } catch (ClassNotFoundException e1) {
    // e1.printStackTrace();
    // System.out.println("LoginController not found");
    // }
    try {
      // 传递的args参数分两个部分:agentJar路径和agentArgs
      // 分别是Agent的JAR包路径和期望传递到服务端的参数 /home/dc/log
      String path = System.getProperty("user.dir");
      System.out.println(path + "  args: " + args);
      if (path != null && path.endsWith("/bin")) {
        path = path.replace("/bin", "");
      }
      // inst.appendToBootstrapClassLoaderSearch(new JarFile(
      // AgentLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile()));
      String configLocation = "";
      String startClass = "";
      boolean sysjar = false;
      System.out.println(currentLoader + "######args is:" + args);
      if (args != null) {
        String[] array = args.trim().split(";");
        for (String param : array) {
          if (param.startsWith("-path:")) {
            path = param.substring(6);
            System.out.println("agent path is:" + path);
          }
        }
        for (String param : array) {
          if (param.startsWith("-conf:")) {// 配置文件
            param = param.substring(7);
            String absouthPath = param.startsWith("/") ? "" : path + File.separator;
            configLocation = absouthPath + param;
          } else if (param.startsWith("-sysjar:")) {// 第三方jar
            sysjar = true;
            param = param.substring(8);
            String[] sysJars = param.split(":");
            String absouthPath = param.startsWith("/") ? "" : path + File.separator;
            for (String s : sysJars) {
              LoadJarUtil.loadJar(absouthPath + s, inst);
            }
          } else if (param.startsWith("-starter:")) {// 启动类
            param = param.substring(9);
            if (!param.contains(".")) {// 全路径
              param = "com.github.apm.core." + param;
            }
            startClass = param;
          }
        }
      }

      if (startClass.length() == 0) {
        startClass = "com.github.apm.core.ApmMonitor";
      }
      if (configLocation.length() == 0) {
        configLocation = path + File.separator + "conf/apm.properties";
      }
      if (!sysjar) {
        System.out.println("jar file path:" + path + File.separator + "lib");
        // Class superClass = webapp.getClass().getSuperclass();// 父类
        // Method[] superMethods = superClass.getDeclaredMethods();
        // for (int j = 0; j < superMethods.length; j++) {
        // superMethods[j].setAccessible(true);// 允许private被访问
        // if (superMethods[j].getName().equals("addURL")) {
        // System.out.println(superMethods[j].getParameterTypes()[0]);
        // System.out.println(" " + superMethods[j].getName() + "=");
        // superMethods[j].invoke(webapp, new URL("file:" + path + File.separator + "lib"
        // + File.separator + "apm_core-jar-with-dependencies.jar"));
        // }
        // }
        LoadJarUtil.loadJar(path + File.separator + "lib" + File.separator, inst);
      }
      currentLoader.loadClass(startClass)
          .getMethod("outerInit", String.class, Instrumentation.class)
          .invoke(null, configLocation, inst);// 启动监听器
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static Method getMethod(Class clazz, String methodName, final Class[] classes)
      throws Exception {
    Method method = null;
    try {
      method = clazz.getDeclaredMethod(methodName, classes);
    } catch (NoSuchMethodException e) {
      try {
        method = clazz.getMethod(methodName, classes);
      } catch (NoSuchMethodException ex) {
        if (clazz.getSuperclass() == null) {
          return method;
        } else {
          method = getMethod(clazz.getSuperclass(), methodName, classes);
        }
      }
    }
    return method;
  }
}
