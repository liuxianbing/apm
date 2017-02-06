package com.github.apm.core.transform.impl;

import static com.github.apm.core.transform.CachedClassLoaderMatcher.cached;
import static com.github.apm.core.transform.CanLoadClassElementMatcher.canLoadClass;
import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.nameMatches;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.github.apm.core.configuration.ApmConfiguration;
import com.github.apm.core.transform.ApmMonitorByteBuddyTransformer;
import com.github.apm.core.util.ClassUtils;
import com.github.apm.core.util.DateUtils;
import com.github.apm.core.util.FileWriterAsync;
import com.github.apm.core.util.NumberUtil;
import com.github.apm.core.util.PropertiesUtil;
import com.github.apm.core.util.PropertyFileConfigurationSource;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class ApmTransformerBasic extends ApmMonitorByteBuddyTransformer {
  public static volatile int slowTime = -1;
  public Map<String, String> properties = ApmConfiguration.getInstance().propertiesMap;

  public ApmTransformerBasic() {
    String slow = properties.get("instrment.time.slow");
    if (null != slow) {
      slowTime = Integer.parseInt(slow);
    }
  }

  public static void writeSlowTime(String classType, String methodName, String signature,
      String returnVal, long spendTime) {
    StringBuffer sb = new StringBuffer(DateUtils.getDate());
    sb.append(classType + "-");
    sb.append(methodName + "-");
    sb.append(signature + "-");
    sb.append("\n" + returnVal);
    sb.append(DateUtils.getDate());
    sb.append(" spend Time:" + spendTime);
    FileWriterAsync.produce(FileWriterAsync.SLOW_TIME, sb.toString());
  }

  @Deprecated
  protected void initConfigure(String path) {
    if (null != path && path.trim().length() > 0) {
      PropertyFileConfigurationSource pfc = new PropertyFileConfigurationSource(path);
      Iterator<Entry<Object, Object>> obj = pfc.getProperties().entrySet().iterator();
      while (obj.hasNext()) {
        Entry<Object, Object> en = obj.next();
        properties.put(en.getKey().toString(), en.getValue().toString());
      }
    }
    if (properties.size() == 0) {
      properties = ApmConfiguration.getInstance().propertiesMap;
    }
  }

  protected ElementMatcher.Junction<MethodDescription.InDefinedShape> generateMethodElementMatcher(
      String key) {
    Map<String, String> map = PropertiesUtil.getFromKeyByPrefix(key, properties);
    if (map.size() > 0) {
      ElementMatcher.Junction<MethodDescription.InDefinedShape> res = none();
      Iterator<Entry<String, String>> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Entry<String, String> en = it.next();
        String[] tmp = en.getValue().split(",");
        ElementMatcher.Junction<MethodDescription.InDefinedShape> matcher = none();
        if (en.getKey().endsWith("named")) {
          for (String cla : tmp)
            matcher = matcher.or(named(cla));
        } else if (en.getKey().endsWith("returnNamed")) {
          for (String cla : tmp)
            matcher = matcher.or(returns(named(cla)));
        } else if (en.getKey().endsWith("nameContains")) {
          for (String cla : tmp)
            matcher = matcher.or(nameContains(cla));
        } else if (en.getKey().endsWith("nameMatches")) {
          for (String cla : tmp)
            matcher = matcher.or(nameMatches(cla));
        } else if (en.getKey().endsWith("nameEndsWith")) {
          for (String cla : tmp)
            matcher = matcher.or(nameEndsWith(cla));
        } else if (en.getKey().endsWith("nameStartsWith")) {
          for (String cla : tmp)
            matcher = matcher.or(nameStartsWith(cla));
        } else if (en.getKey().endsWith("reutrnClass")) {// and
          for (String cla : tmp)
            try {
              matcher = matcher.or(returns(ClassUtils.forNameOrNull(cla)));
            } catch (Exception e) {
              e.printStackTrace();
            }
        } else if (en.getKey().endsWith("arguments")) {// and
          for (String cla : tmp) {
            if (NumberUtil.isNumeric(cla)) {
              matcher = matcher.or(takesArguments(Integer.parseInt(cla)));
            } else {
              String[] className = cla.split(":");
              Class[] cls = new Class[className.length];
              int p = 0;
              for (String n : className) {
                try {
                  cls[p++] = ClassUtils.forNameOrNull(n);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
              matcher = matcher.or(takesArguments(cls));
            }
          }

        } else if (en.getKey().endsWith("decorated")) {// and
          for (String cla : tmp) {
            if (cla.equals("public")) {
              matcher = matcher.or(isPublic());
            } else if (cla.equals("private")) {
              matcher = matcher.or(isPrivate());
            } else if (cla.equals("protected")) {
              matcher = matcher.or(isProtected());
            } else if (cla.equals("static")) {
              matcher = matcher.or(isStatic());
            }
          }
        }
        if (en.getKey().contains("_not_")) {
          matcher = not(matcher);
        }
        if (en.getKey().contains("_and_")) {
          res = res.and(matcher);
        } else {
          res = res.or(matcher);
        }

      }
      return res;
    }
    return null;
  }

  public ElementMatcher.Junction<ClassLoader> generateClassLoaderMatcher() {
    Map<String, String> map = PropertiesUtil.getFromKeyByPrefix("classloader", properties);
    if (map.size() > 0) {
      ElementMatcher.Junction<ClassLoader> raw = none();
      Entry<String, String> en = map.entrySet().iterator().next();
      String[] tmp = en.getValue().split(",");
      for (String cla : tmp) {
        raw = raw.or(cached(canLoadClass(cla)));
      } // end for
      if (en.getKey().endsWith("not")) {
        raw = not(raw);
      }
      return raw;
    } else {
      return super.getClassLoaderMatcher();
    }
  }

  public ElementMatcher.Junction<TypeDescription> generatedTypeMatcher(String key) {
    Map<String, String> map = PropertiesUtil.getFromKeyByPrefix(key, properties);
    if (map.size() > 0) {
      ElementMatcher.Junction<TypeDescription> res = none();
      Iterator<Entry<String, String>> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Entry<String, String> en = it.next();
        String[] tmp = en.getValue().split(",");
        ElementMatcher.Junction<TypeDescription> matcher = none();
        if (en.getKey().endsWith("named")) {
          for (String cla : tmp)
            matcher = matcher.or(named(cla));
        } else if (en.getKey().endsWith("nameContains")) {
          for (String cla : tmp)
            matcher = matcher.or(nameContains(cla));
        } else if (en.getKey().endsWith("nameMatches")) {
          for (String cla : tmp)
            matcher = matcher.or(nameMatches(cla));
        } else if (en.getKey().endsWith("nameEndsWith")) {
          for (String cla : tmp)
            matcher = matcher.or(nameEndsWith(cla));
        } else if (en.getKey().endsWith("nameStartsWith")) {
          for (String cla : tmp)
            matcher = matcher.or(nameStartsWith(cla));
        } else if (en.getKey().endsWith("isSubTypeOf")) {
          for (String cla : tmp)
            try {
              matcher = matcher.and(isSubTypeOf(ClassUtils.forNameOrNull(cla)));
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
        if (en.getKey().contains("_not_")) {
          matcher = not(matcher);
        }
        if (en.getKey().contains("_and_")) {
          res = res.and(matcher);
        } else {
          res = res.or(matcher);
        }
      }
      return res;
    }
    return null;
  }
}
