// package com.github.apm.core.disruptor;
//
// import java.util.ArrayList;
// import java.util.List;
//
// import com.lmax.disruptor.BlockingWaitStrategy;
// import com.lmax.disruptor.EventFactory;
// import com.lmax.disruptor.EventTranslatorTwoArg;
// import com.lmax.disruptor.RingBuffer;
// import com.lmax.disruptor.dsl.Disruptor;
// import com.lmax.disruptor.dsl.ProducerType;
// import com.lmax.disruptor.util.DaemonThreadFactory;
//
// public class ApmDisruptorStarter {
// private static Disruptor<MonditorData> disruptor;
//
// public static RingBuffer<MonditorData> ringBuffer;
//
// static {
// init();
// }
// private static final EventTranslatorTwoArg TRANSLATOR =
// new EventTranslatorTwoArg<MonditorData, Long, String[]>() {
//
// @Override
// public void translateTo(MonditorData event, long sequence, Long spendTime,
// String[] singature) {
// event.spendTime = spendTime;
// event.singature = singature;
// }
//
// };
//
// public static void startUpdate(String[] sing, long spendTime) {
// ringBuffer.publishEvent(TRANSLATOR, spendTime, sing);
// }
//
// static class LongEventFactory implements EventFactory<MonditorData> {
// public MonditorData newInstance() {
// return new MonditorData();
// }
// }
//
// public static void init() {
// disruptor = new Disruptor<MonditorData>(new LongEventFactory(), 2097152,
// DaemonThreadFactory.INSTANCE, ProducerType.MULTI, new BlockingWaitStrategy());
// disruptor.getRingBuffer().next();
//
// List<ApmMonitorDataWorkHandler> list = new ArrayList<ApmMonitorDataWorkHandler>();
// for (int i = 0; i < 4; i++) {
// list.add(new ApmMonitorDataWorkHandler());
// }
// disruptor.handleEventsWithWorkerPool(list.toArray(new ApmMonitorDataWorkHandler[4]));
// ringBuffer = disruptor.start();
// }
// }
