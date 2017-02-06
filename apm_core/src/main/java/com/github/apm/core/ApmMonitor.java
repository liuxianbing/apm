package com.github.apm.core;

import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.apm.core.configuration.ApmConfiguration;
import com.github.apm.core.dispatcher.Dispatcher;
import com.github.apm.core.http.ApmHTTPServer;
import com.github.apm.core.transform.AgentAttacher;
import com.github.apm.core.util.ClassUtils;
import com.github.apm.core.util.PropertyFileConfigurationSource;

public class ApmMonitor {

  public volatile static boolean init = false;
  public volatile static boolean initWeb = false;

  private static List<Runnable> onShutdownActions = new CopyOnWriteArrayList<Runnable>();

  /**
   * 目标程序启动后调用Apm 监控
   */
  public static synchronized void outerInit(String path, final Instrumentation inst) {
    if (!init) {
      initConfigure(path);
      beforeApmServers();
      startWebServer();
      if (null != inst) {
        AgentAttacher.instrumentation = inst;
      }
      if (AgentAttacher.runtimeAttached) {
        AgentAttacher.performRuntimeAttachmentAgain();
      } else {
        onShutdownActions.add(AgentAttacher.performRuntimeAttachment());
        if (ClassUtils.isNotPresent("javax.servlet.Servlet")) {
          Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
              shutDown();
            }
          }));
        }
      }
    }
  }

  /**
   * 通过程序内部调用启动Apm 监控
   */
  public static synchronized void innerInit() {
    if (!init) {
      initConfigure("apm.properties");
      beforeApmServers();
      startWebServer();
      onShutdownActions.add(AgentAttacher.performRuntimeAttachment());
      if (ClassUtils.isNotPresent("javax.servlet.Servlet")) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
          public void run() {
            shutDown();
          }
        }));
      }
    }
  }

  /**
   * 初始化配置文件
   * 
   * @param path
   */
  public static synchronized void initConfigure(String path) {
    PropertyFileConfigurationSource pfc = new PropertyFileConfigurationSource(path);
    Iterator<Entry<Object, Object>> obj = pfc.getProperties().entrySet().iterator();
    while (obj.hasNext()) {
      Entry<Object, Object> en = obj.next();
      ApmConfiguration.getInstance().propertiesMap.put(en.getKey().toString(),
          en.getValue().toString());
    }
    ApmConfiguration.getInstance().initConfig();
  }

  /**
   * 关闭
   */
  public static synchronized void shutDown() {
    for (Runnable onShutdownAction : onShutdownActions) {
      try {
        onShutdownAction.run();
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
    }

    for (ApmServerSPI ass : ServiceLoader.load(ApmServerSPI.class,
        ApmMonitor.class.getClassLoader())) {
      new Thread() {
        public void run() {
          ass.shutDown();
        }
      }.start();
    }
  }

  /**
   * 注入之前启动相关服务
   */
  public static synchronized void beforeApmServers() {
    for (ApmServerSPI ass : ServiceLoader.load(ApmServerSPI.class,
        ApmMonitor.class.getClassLoader())) {
      if (null == Dispatcher.get(ass.getClass().getName() + "-ApmServerSPI")) {
        ass.start();
        Dispatcher.put(ass.getClass().getName() + "-ApmServerSPI", ass);
      }

    }
  }

  public static synchronized void startWebServer() {
    if (!initWeb) {
      if (ApmConfiguration.getInstance().webServerPort != -1) {
        new Thread(new ApmHTTPServer(ApmConfiguration.getInstance().webServerPort,
            ApmConfiguration.getInstance().webPath)).start();
      }
      initWeb = true;
    }
  }
}
