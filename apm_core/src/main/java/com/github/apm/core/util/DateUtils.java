package com.github.apm.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

  public static String getDate() {
    Date d = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    return sdf.format(d);
  }

  public static void main(String[] args) {
    String[] res = new String[3];
    res[0] = "1";
    res[1] = "40000";
    res[2] = "3";
    String[] res1 = new String[2];
    System.arraycopy(res, 0, res1, 0, 2);
    res[1] = "400001";
    for (String s : res1) {
      System.out.println("after:" + s);
    }
  }
}
