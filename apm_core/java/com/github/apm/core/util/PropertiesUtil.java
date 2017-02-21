package com.github.apm.core.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;;

public class PropertiesUtil {

  public static void main(String[] args) {

  }


  public static Map<String, String> getFromKeyByPrefix(String key, Map<String, String> properties) {
    Iterator<Entry<String, String>> it = properties.entrySet().iterator();
    Map<String, String> res = new HashMap<String, String>();
    while (it.hasNext()) {
      Entry<String, String> en = it.next();
      if (en.getKey().startsWith(key)) {
        res.put(en.getKey(), en.getValue());
      }
    }
    return res;
  }

}
