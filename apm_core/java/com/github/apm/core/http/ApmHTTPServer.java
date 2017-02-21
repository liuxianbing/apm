package com.github.apm.core.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

import com.github.apm.core.util.LogUtils;
import com.github.apm.core.util.StringUtils;
import com.github.apm.core.util.TextFormat;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.prometheus.client.CollectorRegistry;

public class ApmHTTPServer implements Runnable {
  private int port;
  private String urlPath;


  public ApmHTTPServer(int port, String urlPath) {
    this.port = port;
    this.urlPath = urlPath;
  }

  public void startWebServer() throws IOException {
    if (!urlPath.startsWith("/")) {
      urlPath = "/" + urlPath;
    }
    HttpServer hs = HttpServer.create(new InetSocketAddress(port), 2);// 设置HttpServer的端口为80
    hs.createContext(urlPath, new MyHandler());// 用MyHandler类内处理到/的请求
    hs.createContext("/log", new LogHandler());
    hs.setExecutor(Executors.newFixedThreadPool(2));// creates a default executor
    hs.start();
  }

  static class LogHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange t) throws IOException {
      OutputStream os = t.getResponseBody();
      Map<String, String> map = StringUtils.getHttpParamters(t.getRequestURI().toString());
      String result = map.toString();
      if (map.size() == 0) {
        result += LogUtils.getAllLogLevel().toString();
      } else {
        String name = map.get("name");
        String level = map.get("level");
        if (name != null && level != null) {
          try {
            LogUtils.setLogLevel(name, level);
            result += "success to set logname:" + name + " to level:" + level;
          } catch (Exception e) {
            result += "errormsg:" + e.getMessage();
          }
        }
      }
      byte[] bb = result.getBytes();
      t.sendResponseHeaders(200, bb.length);
      os.write(bb);
      os.flush();
      os.close();
    }

  }

  static class MyHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      OutputStream os = t.getResponseBody();
      try {
        TextFormat.write004(baos, CollectorRegistry.defaultRegistry.metricFamilySamples());
        t.sendResponseHeaders(200, baos.size());
        os.write(baos.toByteArray());
        os.flush();
        os.close();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        baos.close();
      }
    }
  }

  @Override
  public void run() {
    try {
      startWebServer();
    } catch (Exception e) {
    }
  }
}
