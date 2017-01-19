package com.github.apm.core.configuration;

import java.util.HashMap;
import java.util.Map;

public class ApmConfiguration {

  private ApmConfiguration() {}

  private static ApmConfiguration apmConfig;

  public static synchronized ApmConfiguration getInstance() {
    if (null == apmConfig) {
      apmConfig = new ApmConfiguration();
    }
    return apmConfig;
  }

  public Map<String, String> propertiesMap = new HashMap<>();

  public String globalExclude = "$JaxbAccessor,$$,CGLIB,EnhancerBy,$Proxy,";// apm.instrument.globalExclude
  public String exclude = "com.github.apm";// apm.instrument.exclude
  public String include = "";// apm.instrument.include
  public boolean debug = false;// apm.debug
  public volatile boolean active = true;// apm.monitor.active
  public boolean runtimeAttach = true;// apm..instrument.runtimeAttach
  public String excludedInstrumenter = "";// apm.instrument.excludedInstrumenter
  public String exportGeneratedClassesWithName = "";// apm.instrument.exportGeneratedClassesWithName

  public int webServerPort = 9898;
  public String webPath;
  public String propertiesPath = "";
  public String instrmentTimeProperties = "";

  public String jvmPauseFilePath = "";
  public int jvmPauseWarn = 0;
  public int jvmPauseInfo = 0;
  public String monitorThreads = "";

  public String slowTimeFilePath = "";



  public void initConfig() {
    globalExclude += getByKey("apm.instrument.globalExclude");
    exclude = getByKey("apm.instrument.exclude");
    include = getByKey("apm.instrument.include");
    excludedInstrumenter = getByKey("apm.instrument.excludedInstrumenter");
    exportGeneratedClassesWithName = getByKey("apm.instrument.exportGeneratedClassesWithName");
    debug = Boolean.parseBoolean(getByKey("apm.debug"));
    active = Boolean.parseBoolean(getByKey("apm.monitor.active", "true"));
    runtimeAttach = Boolean.parseBoolean(getByKey("apm..instrument.runtimeAttach", "true"));

    webServerPort = Integer.parseInt(getByKey("apm.webServerPort", "9898"));
    webPath = getByKey("apm.webServer.path", "/prometheus");
    instrmentTimeProperties = getByKey("apm.instrument.instrmentTimeProperties");

    monitorThreads = getByKey("apm.monitorThreads");
    jvmPauseFilePath =
        getByKey("apm.jvmPauseFilePath", System.getProperty("user.dir") + "/jvm_pause.log");
    jvmPauseWarn = Integer.parseInt(getByKey("apm.jvmPauseWarn", "10000"));
    jvmPauseInfo = Integer.parseInt(getByKey("apm.jvmPauseInfo", "1000"));
    slowTimeFilePath =
        getByKey("apm.slowTimeFilePath", System.getProperty("user.dir") + "/slow_time.log");

  }

  private String getByKey(String key) {
    String val = propertiesMap.get(key);
    return val == null ? "" : val;
  }

  private String getByKey(String key, String defaultVal) {
    String val = propertiesMap.get(key);
    return val == null ? defaultVal : val;
  }
}
