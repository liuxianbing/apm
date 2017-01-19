// package com.github.apm.agent;
//
// import java.lang.instrument.Instrumentation;
// import java.util.HashMap;
// import java.util.Iterator;
// import java.util.Map;
// import java.util.Map.Entry;
// import java.util.jar.JarFile;
//
// import com.github.apm.agent.util.FileUtils;
//
/// **
// * Agent切入主类
// *
// * @author liuyang
// *
// */
// public class AgentLauncher2 {
//
// // 全局持有classloader用于隔离Agent实现
// private static volatile ClassLoader apmClassLoader;
//
// private static Map<String, Boolean> loadJarFiles = new HashMap<String, Boolean>();
//
// public static void premain(String args, Instrumentation inst) {
// main(args, inst);
// }
//
// public static void agentmain(String args, Instrumentation inst) {
// main(args, inst);
// }
//
// public synchronized static void resetClassLoader() {
// apmClassLoader = null;
// }
//
// private static ClassLoader loadOrDefineClassLoader(String agentJar) throws Throwable {
// final ClassLoader classLoader;
// // 如果已经被启动则返回之前启动的classloader
// if (null != apmClassLoader) {
// classLoader = apmClassLoader;
// }
// // 如果未启动则重新加载
// else {
// classLoader = new AgentClassLoader(agentJar);
// }
// return apmClassLoader = classLoader;
// }
//
//
//
// private static synchronized void main(final String args, final Instrumentation inst) {
// ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
// Map<String, String> parameters = new HashMap<String, String>();
// try {
// // 传递的args参数分两个部分:agentJar路径和agentArgs
// // 分别是Agent的JAR包路径和期望传递到服务端的参数 /home/dc/log
// String path = System.getProperty("user.dir");
// System.out.println(path + " args: " + args);
// String[] array = args.split(";");
// // final int index = args.indexOf(';');
// // final String agentJar = args.substring(0, index);
// // final String agentArgs = args.substring(index, args.length());
// inst.appendToBootstrapClassLoaderSearch(new JarFile(
// AgentLauncher2.class.getProtectionDomain().getCodeSource().getLocation().getFile()));
// boolean startWebServer = false;
// for (String param : array) {
// if (param.startsWith("-conf")) {// 配置文件
// parameters = FileUtils.readProperties(param.substring(6));
// }
// }
// for (String param : array) {
// if (param.startsWith("-sysjar")) {// 加载到应用程序内部环境
// String[] sysJars = param.substring(7).split(":");
// for (String s : sysJars) {
// if (loadJarFiles.get(s) == null) {
// inst.appendToSystemClassLoaderSearch(
// new JarFile(path + "/" + parameters.get("sys.jar.path") + "/" + s));
// loadJarFiles.put(s, true);
// }
// }
// } else if (param.startsWith("-server") && !startWebServer) {// 新建classloader加载依赖jar包//
// // 构造自定义的类加载器，尽量减少对现有工程的侵蚀
// String[] loadJars = param.substring(7).split(":");
// for (String s : loadJars) {
// if (startWebServer) {
// loadOrDefineClassLoader(path + "/" + parameters.get("server.jar.path") + "/" + s);
// loadJarFiles.put(s, true);
// startWebServer = true;
// }
// }
// }
// }
//
// // 启动自定义收集类
// Iterator<Entry<String, String>> it = parameters.entrySet().iterator();
// while (it.hasNext()) {
// Entry<String, String> en = it.next();
// if (en.getKey().startsWith("prometheus.class")) {
// String[] classmethod = en.getValue().split(":");
// currentLoader.loadClass(classmethod[0]).getMethod(classmethod[1]).invoke(null);
// System.out.println("...overinitialize prometheus :" + en.getValue());
// }
// }
//
// // 启动Web Server
// if (startWebServer && null != parameters.get("server.main")) {
// final Class<?> webServerStarter = apmClassLoader.loadClass(parameters.get("server.main"));
// webServerStarter.getMethod("main", String.class, ClassLoader.class).invoke(null,
// array[array.length - 1], apmClassLoader);
// }
//
// // 启动字节码注入器
// it = parameters.entrySet().iterator();
// while (it.hasNext()) {
// Entry<String, String> en = it.next();
// if (en.getKey().startsWith("apm.injecter")) {
// String[] classmethod = en.getValue().split(":");
// currentLoader.loadClass(classmethod[0])
// .getMethod(classmethod[1], Instrumentation.class, Map.class)
// .invoke(null, inst, parameters);
// System.out.println("...overinitialize inject:" + en.getValue());
// }
// }
// } catch (Throwable e) {
// e.printStackTrace();
// }
// }
// }
