package com.github.apm.core.prometheus;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.prometheus.client.Collector;

/**
 * Exports metrics about JVM thread areas.
 * <p>
 * Example usage:
 * 
 * <pre>
 * {@code
 *   new ThreadExports().register();
 * }
 * </pre>
 * 
 * Example metrics being exported:
 * 
 * <pre>
 *   jvm_threads_current{} 300
 *   jvm_threads_daemon{} 200
 *   jvm_threads_peak{} 410
 *   jvm_threads_started_total{} 1200
 * </pre>
 */
public class ClassLoadingExports extends Collector {
  private static final List<String> EMPTY_LABEL = Collections.emptyList();

  private final ClassLoadingMXBean threadBean;

  public ClassLoadingExports() {
    this(ManagementFactory.getClassLoadingMXBean());
  }

  public ClassLoadingExports(ClassLoadingMXBean threadBean) {
    this.threadBean = threadBean;
  }

  void addThreadMetrics(List<MetricFamilySamples> sampleFamilies) {
    sampleFamilies.add(new MetricFamilySamples("jvm_class_loaded", Type.GAUGE,
        "Current loaded class count of a JVM",
        Collections.singletonList(new MetricFamilySamples.Sample("jvm_class_loaded", EMPTY_LABEL,
            EMPTY_LABEL, threadBean.getLoadedClassCount()))));

    sampleFamilies.add(
        new MetricFamilySamples("jvm_class_totalLoaded", Type.GAUGE, "Total class count of a JVM",
            Collections.singletonList(new MetricFamilySamples.Sample("jvm_class_totalLoaded",
                EMPTY_LABEL, EMPTY_LABEL, threadBean.getTotalLoadedClassCount()))));

    sampleFamilies
        .add(new MetricFamilySamples("jvm_class_unload", Type.GAUGE, "unload class count of a JVM",
            Collections.singletonList(new MetricFamilySamples.Sample("jvm_class_unload",
                EMPTY_LABEL, EMPTY_LABEL, threadBean.getUnloadedClassCount()))));

  }


  public List<MetricFamilySamples> collect() {
    List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
    addThreadMetrics(mfs);
    return mfs;
  }
}
