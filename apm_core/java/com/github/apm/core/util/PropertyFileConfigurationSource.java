package com.github.apm.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * Loads a properties file from classpath. Falls back to loading from file system.
 *
 */
public final class PropertyFileConfigurationSource {

  private Properties properties;

  private final String location;

  public PropertyFileConfigurationSource(String location) {
    this.location = location;
    reload();
  }



  public Properties getProperties() {
    return properties;
  }



  public void reload() {
    properties = getProperties(location);
    if (properties == null) {
      properties = new Properties();
    }
  }

  public boolean isSavingPersistent() {
    return true;
  }

  public String getName() {
    return location;
  }

  public static boolean isPresent(String location) {
    return getProperties(location) != null;
  }

  private static Properties getProperties(String location) {
    if (location == null) {
      return null;
    }
    Properties props = getFromClasspath(location);
    if (props == null) {
      props = getFromFileSystem(location);
    }
    return props;
  }

  private static Properties getFromClasspath(String classpathLocation) {
    final Properties props = new Properties();
    InputStream resourceStream = PropertyFileConfigurationSource.class.getClassLoader()
        .getResourceAsStream(classpathLocation);
    if (resourceStream != null) {
      try {
        props.load(resourceStream);
        return props;
      } catch (IOException e) {
      } finally {
        try {
          resourceStream.close();
        } catch (IOException e) {
        }
      }
    }
    return null;
  }

  private static Properties getFromFileSystem(String location) {
    Properties props = new Properties();
    InputStream input = null;
    try {
      input = new FileInputStream(location);
      props.load(input);
      return props;
    } catch (FileNotFoundException ex) {
      return null;
    } catch (IOException e) {
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
        }
      }
    }
    return null;
  }

  public String getValue(String key) {
    return properties.getProperty(key);
  }

  public boolean isSavingPossible() {
    return true;
  }

  public void save(String key, String value) throws IOException {
    synchronized (this) {
      properties.put(key, value);
      try {
        URL resource = getClass().getClassLoader().getResource(location);
        if (resource == null) {
          resource = new URL("file://" + location);
        }
        if (!"file".equals(resource.getProtocol())) {
          throw new IOException(
              "Saving to property files inside a war, ear or jar is not possible.");
        }
        File file = new File(resource.toURI());
        final FileOutputStream out = new FileOutputStream(file);
        properties.store(out, null);
        out.flush();
        out.close();
      } catch (URISyntaxException e) {
        throw new IOException(e);
      }
    }
  }

}
