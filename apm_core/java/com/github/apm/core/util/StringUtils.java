package com.github.apm.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringUtils {

  public static String[] splitWorker(String str, char separatorChar, boolean preserveAllTokens) {
    // Performance tuned for 2.0 (JDK1.4)

    if (str == null) {
      return null;
    }
    int len = str.length();
    if (len == 0) {
      return null;
    }
    List<String> list = new ArrayList<String>();
    int i = 0, start = 0;
    boolean match = false;
    boolean lastMatch = false;
    while (i < len) {
      if (str.charAt(i) == separatorChar) {
        if (match || preserveAllTokens) {
          list.add(str.substring(start, i));
          match = false;
          lastMatch = true;
        }
        start = ++i;
        continue;
      }
      lastMatch = false;
      match = true;
      i++;
    }
    if (match || preserveAllTokens && lastMatch) {
      list.add(str.substring(start, i));
    }
    return list.toArray(new String[list.size()]);
  }

  public static void main(String[] args) {
    System.out.println(getHttpParamters(""));
  }

  public static Map<String, String> getHttpParamters(String urls) {
    int ind = urls.indexOf("?");
    Map<String, String> map = new HashMap<>();
    if (ind > 0) {
      String[] param = urls.substring(ind + 1).split("&");
      for (String s : param) {
        String[] ak = s.split("=", 2);
        map.put(ak[0], ak[1]);
      }
    }
    return map;
  }
}
