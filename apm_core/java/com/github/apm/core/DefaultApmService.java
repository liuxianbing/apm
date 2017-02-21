package com.github.apm.core;

import com.github.apm.core.configuration.ApmConfiguration;
import com.github.apm.core.prometheus.ClassLoadingExports;
import com.github.apm.core.prometheus.DefaultThreadExports;
import com.github.apm.core.prometheus.JvmPauseMonitor;
import com.github.apm.core.prometheus.PrometheusMetricsModule;
import com.github.apm.core.util.FileWriterAsync;

import io.prometheus.client.hotspot.DefaultExports;

public class DefaultApmService implements ApmServerSPI {

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void start() {
    DefaultExports.initialize();
    PrometheusMetricsModule.initPrometheus(ApmConfiguration.getInstance().propertiesMap);
    if (ApmConfiguration.getInstance().monitorThreads.length() > 0) {
      new DefaultThreadExports(ApmConfiguration.getInstance().monitorThreads.split(","),
          "apm-threads-default").register();
    }
    new ClassLoadingExports().register();
    new JvmPauseMonitor(ApmConfiguration.getInstance().jvmPauseWarn,
        ApmConfiguration.getInstance().jvmPauseInfo,
        ApmConfiguration.getInstance().jvmPauseFilePath).start();
  }

  @Override
  public void shutDown() {
    FileWriterAsync.shutDown();
  }

}
