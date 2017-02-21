package com.github.apm.core.prometheus;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

public class PrometheusMetricsModule {


  /**
   * 创建延时模型
   * 
   * @return
   */
  public static Histogram getHistogram() {
    return histogram;
  }

  public static void jvmPauseCounter(String labels, long millonseconds) {
    try {
      jvmPauseCounter.labels(labels).inc(millonseconds);
    } catch (Exception e) {
    }
  }

  /**
   * 请求计数+1
   * 
   * @param labelValues label值，按定义顺序来
   */
  public static void requestCounter(String className, String methodName, String paramters) {
    try {
      if (counterLabelSize == 0) {
        counter.inc();
      } else {
        String[] res = new String[counterLabelSize];
        res[0] = className;
        if (histogramLabelSize == 3) {
          res[1] = methodName;
          res[2] = paramters;
        } else if (histogramLabelSize == 2) {
          res[1] = methodName;
        }
        counter.labels(res).inc();
      }
    } catch (Exception e) {
    }
  }

  /**
   * error计数+1
   * 
   * @param labelValues label值，按定义顺序来
   */
  public static void errorCounter(String className, String methodName, String paramters,
      Throwable t) {
    if (errorCounterLabelSize == 0) {
      errorCounter.inc();
    } else {
      String[] res = new String[errorCounterLabelSize];
      res[0] = t.getMessage();
      if (histogramLabelSize == 3) {
        res[1] = className;
        res[2] = methodName;
      } else if (histogramLabelSize == 2) {
        res[1] = className;
      }
      errorCounter.labels(res).inc();
    }
  }

  private static String[] sliceArray(String[] orign, int size) {
    String[] dest = new String[size];
    System.arraycopy(orign, 0, dest, 0, size);
    return dest;
  }

  public static void calLate(long spendTime, String[] res) {
    histogram.labels(res).observe(spendTime);
  }

  /**
   * 计算耗时
   */
  public static void calLate(long spendTime, String className, String methodName,
      String paramters) {
    if (histogramLabelSize > 0) {
      String[] res = new String[histogramLabelSize];
      res[0] = className;
      if (histogramLabelSize == 3) {
        res[1] = methodName;
        res[2] = paramters;
      } else if (histogramLabelSize == 2) {
        res[1] = methodName;
      }
      histogram.labels(res).observe(spendTime);
    } else {
      histogram.observe(spendTime);
    }
  }


  private static Counter jvmPauseCounter = null;
  // 请求数计数模型
  private static Counter counter = null;
  private static int counterLabelSize = 0;
  // 延时模型
  private static Histogram histogram = null;
  private static volatile int histogramLabelSize = 0;

  // 系统error计数模型
  private static Counter errorCounter;
  private static int errorCounterLabelSize = 0;

  private static void checkProperties(String[] val) {
    if (val.length == 4) {
      if (val[2].split(",").length > 3) {
        throw new RuntimeException("prometheus properties error ,labels size mast <=3");
      }
    } else if (val.length < 3) {
      throw new RuntimeException("prometheus properties error");
    }
  }

  public static void initPrometheus(Map<String, String> properties) {
    Iterator<Entry<String, String>> en = properties.entrySet().iterator();
    String namespace = properties.get("prometheus.namespace");
    while (en.hasNext()) {
      Entry<String, String> it = en.next();
      String[] val = it.getValue().split("\\|");
      if (it.getKey().startsWith("prometheus.counter")) {
        checkProperties(val);
        if (val.length == 4) {
          counterLabelSize = val[2].split(",").length;
          counter = Counter.build().namespace(namespace).subsystem(val[0]).name(val[1])
              .labelNames(val[2].split(",")).help(val[3]).register();
        } else if (val.length == 3) {
          counter = Counter.build().namespace(namespace).subsystem(val[0]).name(val[1]).help(val[2])
              .register();
        }
      } else if (it.getKey().startsWith("prometheus.time")) {
        checkProperties(val);
        if (val.length == 4) {
          histogramLabelSize = val[2].split(",").length;
          histogram = Histogram.build().namespace(namespace).subsystem(val[0]).name(val[1])
              .labelNames(val[2].split(",")).help(val[3]).register();
        } else if (val.length == 3) {
          histogram = Histogram.build().namespace(namespace).subsystem(val[0]).name(val[1])
              .help(val[3]).register();
        }
      } else if (it.getKey().startsWith("prometheus.error")) {
        checkProperties(val);
        if (val.length == 4) {
          errorCounterLabelSize = val[2].split(",").length;
          errorCounter = Counter.build().namespace(namespace).subsystem(val[0]).name(val[1])
              .labelNames(val[2].split(",")).help(val[3]).register();
        } else if (val.length == 3) {
          errorCounter = Counter.build().namespace(namespace).subsystem(val[0]).name(val[1])
              .help(val[2]).register();
        }
      } else if (it.getKey().startsWith("prometheus.jvmpause")) {
        if (val.length == 4) {
          // MAP.put("jvmPauseCounter", val[2].split(",").length);
          jvmPauseCounter = Counter.build().namespace(namespace).subsystem(val[0]).name(val[1])
              .labelNames(val[2].split(",")).help(val[3]).register();
        }
      }
    }
  }
}
