/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.apm.core.prometheus;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.apm.core.util.DateUtils;
import com.github.apm.core.util.FileWriterAsync;
import com.github.apm.core.util.Stopwatch;


/**
 * Class which sets up a simple thread which runs in a loop sleeping for a short interval of time.
 * If the sleep takes significantly longer than its target time, it implies that the JVM or host
 * machine has paused processing, which may cause other problems. If such a pause is detected, the
 * thread logs a message. The original JvmPauseMonitor is:
 * ${hadoop-common-project}/hadoop-common/src/main/java/org/apache/hadoop/util/ JvmPauseMonitor.java
 * r1503806 | cmccabe | 2013-07-17 01:48:24 +0800 (Wed, 17 Jul 2013) | 1 line HADOOP-9618. thread
 * which detects GC pauses(Todd Lipcon)
 */
public class JvmPauseMonitor {

  /** The target sleep time */
  private static final long SLEEP_INTERVAL_MS = 500;

  /** log WARN if we detect a pause longer than this threshold */
  private final long warnThresholdMs;
  private static final long WARN_THRESHOLD_DEFAULT = 10000;

  /** log INFO if we detect a pause longer than this threshold */
  private final long infoThresholdMs;
  private static final long INFO_THRESHOLD_DEFAULT = 1000;

  private Thread monitorThread;
  private volatile boolean shouldRun = true;
  private String path;

  public JvmPauseMonitor(long warn, int info, String path) {
    this.warnThresholdMs = warn > 0 ? warn : WARN_THRESHOLD_DEFAULT;
    this.infoThresholdMs = info > 0 ? info : INFO_THRESHOLD_DEFAULT;
    this.path = path;
  }

  public void start() {
    monitorThread = new Thread(new Monitor());
    monitorThread.setDaemon(true);
    monitorThread.setName("JvmPauseMonitor");
    monitorThread.start();
  }

  public void stop() {
    shouldRun = false;
    monitorThread.interrupt();
    try {
      monitorThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private String formatMessage(long extraSleepTime, Map<String, GcTimes> gcTimesAfterSleep,
      Map<String, GcTimes> gcTimesBeforeSleep) {
    Set<String> gcBeanNames = new HashSet<>();
    gcBeanNames.addAll(gcTimesAfterSleep.keySet());
    gcBeanNames.addAll(gcTimesBeforeSleep.keySet());
    List<String> gcDiffs = new ArrayList<>();
    for (String name : gcBeanNames) {
      GcTimes diff = gcTimesAfterSleep.get(name).subtract(gcTimesBeforeSleep.get(name));
      if (diff.gcCount != 0) {
        gcDiffs.add("GC pool '" + name + "' had collection(s): " + diff.toString());
        PrometheusMetricsModule.jvmPauseCounter(name, diff.gcTimeMillis);
      }
    }
    StringBuffer ret = new StringBuffer("Detected pause in JVM or host machine (eg GC): "
        + "pause of approximately " + extraSleepTime + "ms\n");
    PrometheusMetricsModule.jvmPauseCounter("JVM Pause", extraSleepTime);
    if (gcDiffs.isEmpty()) {
      ret.append("No GCs detected");
    } else {
      for (String s : gcDiffs)
        ret.append(s + "\n");
    }
    return ret.toString();
  }

  private Map<String, GcTimes> getGcTimes() {
    Map<String, GcTimes> map = new HashMap<>();
    List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    for (GarbageCollectorMXBean gcBean : gcBeans) {
      map.put(gcBean.getName(), new GcTimes(gcBean));
    }
    return map;
  }

  private static class GcTimes {
    private GcTimes(GarbageCollectorMXBean gcBean) {
      gcCount = gcBean.getCollectionCount();
      gcTimeMillis = gcBean.getCollectionTime();
    }

    private GcTimes(long count, long time) {
      this.gcCount = count;
      this.gcTimeMillis = time;
    }

    private GcTimes subtract(GcTimes other) {
      return new GcTimes(this.gcCount - other.gcCount, this.gcTimeMillis - other.gcTimeMillis);
    }

    @Override
    public String toString() {
      return "count=" + gcCount + " time=" + gcTimeMillis + "ms";
    }

    private long gcCount;
    private long gcTimeMillis;
  }

  private class Monitor implements Runnable {
    @Override
    public void run() {
      Stopwatch sw = new Stopwatch();
      Map<String, GcTimes> gcTimesBeforeSleep = getGcTimes();
      while (shouldRun) {
        sw.reset().start();
        try {
          Thread.sleep(SLEEP_INTERVAL_MS);
        } catch (InterruptedException ie) {
          return;
        }
        long extraSleepTime = sw.elapsedMillis() - SLEEP_INTERVAL_MS;
        Map<String, GcTimes> gcTimesAfterSleep = getGcTimes();

        if (extraSleepTime > warnThresholdMs) {
          FileWriterAsync.produce(FileWriterAsync.JVM_PAUSE, "warn-" + DateUtils.getDate()
              + formatMessage(extraSleepTime, gcTimesAfterSleep, gcTimesBeforeSleep));
        } else if (extraSleepTime > infoThresholdMs) {
          FileWriterAsync.produce(FileWriterAsync.JVM_PAUSE, "info-" + DateUtils.getDate()
              + formatMessage(extraSleepTime, gcTimesAfterSleep, gcTimesBeforeSleep));
        }
        gcTimesBeforeSleep = gcTimesAfterSleep;
      }
    }
  }

  /**
   * Simple 'main' to facilitate manual testing of the pause monitor.
   * 
   * This main function just leaks memory into a list. Running this class with a 1GB heap will very
   * quickly go into "GC hell" and result in log messages about the GC pauses.
   */
  public static void main(String[] args) throws Exception {}
  // new JvmPauseMonitor(0, 0).start();
  // List<String> list = new ArrayList<>();
  // int i = 0;
  // while (true) {
  // list.add(String.valueOf(i++));
  // }
  // }
}
