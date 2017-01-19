package com.github.apm.core.prometheus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

public class DefaultThreadExports extends Collector {
  public String[] threadName;
  public static List<String> labelValue = new ArrayList<>();
  public static List<String> labelName = new ArrayList<>();
  private String name;

  public DefaultThreadExports(String[] threadName, String name) {
    this.threadName = threadName;
    labelName.add("Thread Name");
    this.name = name;
  }

  private boolean checkIn(String name) {
    for (String s : threadName) {
      if (name.startsWith(s)) {
        return true;
      }
    }
    return false;
  }

  void addThreadMetrics(List<MetricFamilySamples> sampleFamilies) {
    Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
    List<Sample> list = new ArrayList<>();
    Map<String, Integer> temp = new HashMap<>();
    if (map != null) {
      Iterator<Thread> it = map.keySet().iterator();
      while (it.hasNext()) {
        Thread thread = it.next();
        if (thread.isAlive() && checkIn(thread.getName())) {
          String[] value = thread.getName().split("-");
          if (value != null && value.length >= 2) {
            if (temp.get(value[1]) != null) {
              temp.put(value[1], temp.get(value[1]) + 1);
            } else {
              temp.put(value[1], 1);
            }
          }

        }
      }
    }
    if (temp.size() > 0) {
      Iterator<String> tempIt = temp.keySet().iterator();
      while (tempIt.hasNext()) {
        labelValue.clear();
        String ip = tempIt.next();
        labelValue.add(ip);
        list.add(new MetricFamilySamples.Sample(name, labelName, labelValue, temp.get(ip)));
      }
    }
    sampleFamilies.add(new MetricFamilySamples(name, Type.GAUGE, "Current thread count ", list));

  }

  public List<MetricFamilySamples> collect() {
    List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
    addThreadMetrics(mfs);
    return mfs;
  }
}
