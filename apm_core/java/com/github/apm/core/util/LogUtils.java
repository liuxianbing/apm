package com.github.apm.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.apm.core.transform.ApmMonitorByteBuddyTransformer;


public class LogUtils {
  public static final String LOG_FACTORY = "org.slf4j.LoggerFactory";

  public static final String LOGBACK_CLASSIC = "ch.qos.logback.classic";
  public static final String LOGBACK_CLASSIC_LOGGER = "ch.qos.logback.classic.Logger";
  public static final String LOGBACK_CLASSIC_LEVEL = "ch.qos.logback.classic.Level";

  public static final String LOG4J_CLASSIC = "org.apache.log4j";
  public static final String LOG4J_CLASSIC_LOGGER = "org.apache.log4j.Logger";
  public static final String LOG4J_CLASSIC_LEVEL = "org.apache.log4j.Level";

  private static ClassLoader classLoader = null;

  static {
    if (ApmMonitorByteBuddyTransformer.appClassLoader != null) {
      classLoader = ApmMonitorByteBuddyTransformer.appClassLoader;
    } else {
      classLoader = LogUtils.class.getClassLoader();
    }
  }

  public static boolean setLog4j2Level(String loggerName, String logLevel) {
    String logLevelUpper = (logLevel == null) ? "OFF" : logLevel.toUpperCase();
    Map<String, Object> map = getAllLogLevelFromLog4J2();
    try {
      if (map.get(loggerName) == null) {
        throw new RuntimeException("No logger for the name:" + loggerName);
      }
      Object logLevelObj = getFieldVaulue("org.apache.logging.log4j.Level", logLevelUpper);
      if (logLevelObj == null) {
        throw new RuntimeException("No such log level: :" + logLevelUpper);
      }

      Class<?>[] paramTypes = {logLevelObj.getClass()};
      Object[] params = {logLevelObj};

      Method method = map.get(loggerName).getClass().getMethod("setLevel", paramTypes);
      method.invoke(map.get(loggerName), params);

      Class<?> clazz = classLoader.loadClass("org.apache.log4j.LogManager");
      Object loggerContext = clazz.getMethod("getContext", Boolean.class).invoke(null, false);
      loggerContext.getClass().getMethod("updateLoggers").invoke(loggerContext);
    } catch (Exception e) {
      throw new RuntimeException(
          "Couldn't set log4j level to" + logLevelUpper + "for the logger " + loggerName);
    }
    return true;
  }

  public static boolean setLog4jLevel(String loggerName, String logLevel) {
    String logLevelUpper = (logLevel == null) ? "OFF" : logLevel.toUpperCase();
    try {
      Class<?> clz = Class.forName(LOG4J_CLASSIC_LOGGER);
      // Obtain logger by the name
      Object loggerObtained;
      if ((loggerName == null) || loggerName.trim().isEmpty()) {
        // Use ROOT logger if given logger name is blank.
        Method method = clz.getMethod("getRootLogger");
        loggerObtained = method.invoke(null);
        loggerName = "ROOT";
      } else {
        Method method = clz.getMethod("getLogger", String.class);
        loggerObtained = method.invoke(null, loggerName);
      }
      if (loggerObtained == null) {
        throw new RuntimeException("No logger for the name:" + loggerName);
      }
      Object logLevelObj = getFieldVaulue(LOG4J_CLASSIC_LEVEL, logLevelUpper);
      if (logLevelObj == null) {
        throw new RuntimeException("No such log level: :" + logLevelUpper);
      }

      Class<?>[] paramTypes = {logLevelObj.getClass()};
      Object[] params = {logLevelObj};

      Method method = clz.getMethod("setLevel", paramTypes);
      method.invoke(loggerObtained, params);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(
          "Couldn't set log4j level to" + logLevelUpper + "for the logger " + loggerName);
    }
  }

  public static boolean setLogBackLevel(String loggerName, String logLevel) {
    String logLevelUpper = (logLevel == null) ? "OFF" : logLevel.toUpperCase();
    try {
      // Use ROOT logger if given logger name is blank.
      if ((loggerName == null) || loggerName.trim().isEmpty()) {
        loggerName = (String) getFieldVaulue(LOGBACK_CLASSIC_LOGGER, "ROOT_LOGGER_NAME");
      }

      // Obtain logger by the name
      Object loggerObtained = getLogObject(loggerName);
      if (loggerObtained == null) {
        throw new RuntimeException("No logger for the name:" + loggerName);
      }

      Object logLevelObj = getFieldVaulue(LOGBACK_CLASSIC_LEVEL, logLevelUpper);
      if (logLevelObj == null) {
        throw new RuntimeException("No such log level: :" + logLevelUpper);
      }

      Class<?>[] paramTypes = {logLevelObj.getClass()};
      Object[] params = {logLevelObj};

      Class<?> clz = classLoader.loadClass(LOGBACK_CLASSIC_LOGGER);
      Method method = clz.getMethod("setLevel", paramTypes);
      method.invoke(loggerObtained, params);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(
          "Couldn't set log4j level to" + logLevelUpper + "for the logger " + loggerName);
    }
  }

  private static Object getLogObject(String name) {
    try {
      Class<?> clazz = classLoader.loadClass(LOG_FACTORY);
      return clazz.getMethod("getLogger", String.class).invoke(null, name);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static Object getFieldVaulue(String fullClassName, String fieldName) {
    try {
      Class<?> clazz = classLoader.loadClass(fullClassName);
      Field field = clazz.getField(fieldName);
      return field.get(null);
    } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
        | NoSuchFieldException | SecurityException ignored) {
      return null;
    }
  }

  public static boolean setLogLevel(String loggerName, String logLevel) {
    if (checkLogType(LOGBACK_CLASSIC_LOGGER)) {
      return setLogBackLevel(loggerName, logLevel);
    } else if (checkLogType("org.apache.logging.log4j.core.config.LoggerConfig")) {
      return setLog4j2Level(loggerName, logLevel);
    } else if (checkLogType("org.apache.log4j.Logger")) {
      return setLog4jLevel(loggerName, logLevel);
    }
    throw new RuntimeException("not found log jar");
  }

  public static Map<String, Object> getAllLogLevel() {
    if (checkLogType(LOGBACK_CLASSIC_LOGGER)) {
      return getAllLogLevelFromLogBack();
    } else if (checkLogType("org.apache.logging.log4j.core.config.LoggerConfig")) {
      return getAllLogLevelFromLog4J2();
    } else if (checkLogType("org.apache.log4j.Logger")) {
      return getAllLogLevelFromLog4J();
    }
    return new HashMap<>();
  }

  private static boolean checkLogType(String className) {
    try {
      classLoader.loadClass(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }


  public static Map<String, Object> getAllLogLevelFromLog4J2() {
    Map<String, Object> res = new HashMap<>();
    try {
      Class<?> clazz = classLoader.loadClass("org.apache.log4j.LogManager");
      Object loggerContext = clazz.getMethod("getContext", Boolean.class).invoke(null, false);
      Class<?> contextClass = loggerContext.getClass();
      Object config = contextClass.getMethod("getConfiguration").invoke(loggerContext);
      Class<?> configClass = config.getClass();
      Map<String, Object> map =
          (Map<String, Object>) configClass.getMethod("getLoggers").invoke(config);
      for (Object obj : map.values()) {
        String key = (String) obj.getClass().getMethod("getName").invoke(obj);
        if (null == key || key.length() == 0) {
          key = "root";
        }
        res.put(key, obj.getClass().getMethod("getLevel").invoke(obj));
      }
    } catch (Exception e) {
    }
    return res;
  }

  public static Map<String, Object> getAllLogLevelFromLog4J() {
    Map<String, Object> res = new HashMap<>();
    try {
      Class<?> clazz = classLoader.loadClass("org.apache.log4j.LogManager");
      Object obj = clazz.getMethod("getRootLogger").invoke(null);
      // Class<?> logerClass = classLoader.loadClass("org.apache.log4j.Logger");
      Class<?> logerClass = obj.getClass();
      Object tmp = logerClass.getMethod("getLevel").invoke(obj);
      res.put((String) logerClass.getMethod("getName").invoke(obj), tmp);
      Enumeration enumeration = (Enumeration) clazz.getMethod("getCurrentLoggers").invoke(null);
      while (enumeration.hasMoreElements()) {
        obj = enumeration.nextElement();
        tmp = logerClass.getMethod("getLevel").invoke(obj);
        if (null != tmp) {
          res.put((String) logerClass.getMethod("getName").invoke(obj), tmp);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return res;
  }

  public static Map<String, Object> getAllLogLevelFromLogBack() {
    Map<String, Object> res = new HashMap<>();
    try {
      Class<?> clazz = classLoader.loadClass(LOG_FACTORY);
      Object rootLog = clazz.getMethod("getLogger", String.class).invoke(null, "ROOT");
      // Class<?> logerClass = classLoader.loadClass("ch.qos.logback.classic.Logger");
      Class<?> logerClass = rootLog.getClass();
      res.put((String) logerClass.getMethod("getName").invoke(rootLog),
          logerClass.getMethod("getLevel").invoke(rootLog));
      Object loggerContext = clazz.getMethod("getILoggerFactory").invoke(null);
      // clazz = classLoader.loadClass("ch.qos.logback.classic.LoggerContext");
      List<Object> list =
          (List<Object>) loggerContext.getClass().getMethod("getLoggerList").invoke(loggerContext);
      for (Object obj : list) {
        Object tmp = logerClass.getMethod("getLevel").invoke(obj);
        if (null != tmp) {
          res.put((String) logerClass.getMethod("getName").invoke(obj), tmp);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return res;
  }
}
