// package com.github.apm.core.disruptor;
//
// import com.lmax.disruptor.EventTranslatorOneArg;
// import com.lmax.disruptor.RingBuffer;
//
// public class MonitorDataProducer {
// private RingBuffer<MonditorData> ringBuffer;
//
// public MonitorDataProducer(RingBuffer<MonditorData> ringBuffer) {
// this.ringBuffer = ringBuffer;
// }
//
// private final EventTranslatorOneArg TRANSLATOR =
// new EventTranslatorOneArg<MonditorData, Object[]>() {
// @Override
// public void translateTo(MonditorData event, long sequence, Object[] data) {
// event.spendTime = (long) data[0];
// event.singature = (String[]) data[1];
// }
// };
//
// public void onData(long spandTime, Object[] obj) {
// ringBuffer.publishEvent(TRANSLATOR, obj);
// }
// }
