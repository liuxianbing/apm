// package com.github.apm.core.disruptor;
//
// import com.github.apm.core.prometheus.PrometheusMetricsModule;
// import com.lmax.disruptor.WorkHandler;
//
// public class ApmMonitorDataWorkHandler implements WorkHandler<MonditorData> {
//
// @Override
// public void onEvent(MonditorData event) throws Exception {
// // TODO Auto-generated method stub
// PrometheusMetricsModule.histogram.labels(event.singature).observe(event.spendTime);
// }
//
// }
