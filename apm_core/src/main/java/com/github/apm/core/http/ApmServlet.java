package com.github.apm.core.http;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

public class ApmServlet extends HttpServlet {

  private static final long serialVersionUID = -6821053293909324101L;

  public void init() throws ServletException {
    super.init();
  }

  @Override
  public void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    PrintWriter write = response.getWriter();
    try {
      TextFormat.write004(write, CollectorRegistry.defaultRegistry.metricFamilySamples());
      write.flush();
      write.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
