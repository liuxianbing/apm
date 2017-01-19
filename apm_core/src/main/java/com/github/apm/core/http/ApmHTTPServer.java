package com.github.apm.core.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

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
    hs.setExecutor(Executors.newFixedThreadPool(2));// creates a default executor
    hs.start();
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
