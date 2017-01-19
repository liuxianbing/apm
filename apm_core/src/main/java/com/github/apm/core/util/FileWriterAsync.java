package com.github.apm.core.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.apm.core.configuration.ApmConfiguration;

public class FileWriterAsync {

  private static final LinkedBlockingQueue<String[]> queneData = new LinkedBlockingQueue<>(500);

  public static final String JVM_PAUSE = "pause";
  public static final String SLOW_TIME = "slow";

  private static Map<String, FileWriter> map = new HashMap<>();
  private static Map<String, Integer> mapInit = new HashMap<>();
  private static Map<String, String> mapPath = new HashMap<>();
  static {
    mapPath.put(JVM_PAUSE, ApmConfiguration.getInstance().jvmPauseFilePath);
    mapPath.put(SLOW_TIME, ApmConfiguration.getInstance().slowTimeFilePath);
    Thread t = new Thread(new Consumer());
    t.setName("APM-FileWriter");
    t.setDaemon(true);
    t.start();
  }

  public static void shutDown() {
    Iterator<Entry<String, FileWriter>> en = map.entrySet().iterator();
    while (en.hasNext()) {
      try {
        en.next().getValue().close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void produce(String key, String val) {
    String[] obj = new String[2];
    obj[0] = key;
    obj[1] = val;
    try {
      queneData.offer(obj);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private static FileWriter initWriter(String key) {
    String path = mapPath.get(key);
    if (mapInit.get(key) != null && mapInit.get(key) == -1) {
      return null;
    }
    if (null != path) {
      try {
        map.put(key, new FileWriter(path, true));
        mapInit.put(key, 1);
      } catch (IOException e) {
        e.printStackTrace();
        mapInit.put(key, -1);
      }
    }
    return map.get(key);
  }

  public static void writeContent(String key, String obj) {
    try {
      FileWriter writer = map.get(key);
      if (writer == null) {
        writer = initWriter(key);
      }
      if (writer != null) {
        writer.write(obj);
        writer.flush();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static class Consumer implements Runnable {
    public void run() {
      try {
        while (true) {
          String[] obj = queneData.take();
          writeContent(obj[0], obj[1]);
        }
      } catch (Exception e) {
      }
    }
  }
}
