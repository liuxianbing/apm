package com.github.apm.core.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FileUtils {


  public static Map<String, String> readProperties(String path) {
    Map<String, String> map = new HashMap<>();
    try {
      List<String> list = readLines(new FileInputStream(new File(path)));
      for (String s : list) {
        String line = s.trim();
        if (!line.startsWith("#")) {
          String[] arr = line.split("=", 2);
          if (arr.length == 2) {
            map.put(arr[0].trim(), arr[1].trim());
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return map;
  }

  public static FileInputStream openInputStream(File file) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canRead() == false) {
        throw new IOException("File '" + file + "' cannot be read");
      }
    } else {
      throw new FileNotFoundException("File '" + file + "' does not exist");
    }
    return new FileInputStream(file);
  }


  public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canWrite() == false) {
        throw new IOException("File '" + file + "' cannot be written to");
      }
    } else {
      File parent = file.getParentFile();
      if (parent != null) {
        if (!parent.mkdirs() && !parent.isDirectory()) {
          throw new IOException("Directory '" + parent + "' could not be created");
        }
      }
    }
    return new FileOutputStream(file, append);
  }

  public static void writeStringToFile(File file, String data, boolean append) {
    OutputStream out = null;
    try {
      out = openOutputStream(file, append);
      write(data, out);
      out.close(); // don't swallow close Exception if copy completes normally
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      closeQuietly(out);
    }
  }

  public static void writeStringToFile(File file, byte[] data, boolean append) {
    OutputStream out = null;
    try {
      out = openOutputStream(file, append);
      out.write(data);
      out.close(); // don't swallow close Exception if copy completes normally
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      closeQuietly(out);
    }
  }

  public static void write(String data, OutputStream output) throws IOException {
    if (data != null) {
      output.write(data.getBytes("utf-8"));
    }
  }

  public static List<String> readLines(File file, String encoding) throws IOException {
    InputStream in = null;
    try {
      in = openInputStream(file);
      return readLines(in, encoding);
    } finally {
      closeQuietly(in);
    }
  }

  public static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ioe) {
      // ignore
    }
  }

  public static void closeQuietly(Reader input) {
    closeQuietly((Closeable) input);
  }

  public static List<String> readLines(InputStream input, String encoding) throws IOException {
    if (encoding == null) {
      return readLines(input);
    } else {
      InputStreamReader reader = new InputStreamReader(input, encoding);
      return readLines(reader);
    }
  }

  public static List<String> readLines(InputStream input) throws IOException {
    InputStreamReader reader = new InputStreamReader(input);
    return readLines(reader);
  }

  public static BufferedReader toBufferedReader(Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }

  public static List<String> readLines(Reader input) throws IOException {
    BufferedReader reader = toBufferedReader(input);
    List<String> list = new ArrayList<String>();
    String line = reader.readLine();
    while (line != null) {
      list.add(line);
      line = reader.readLine();
    }
    return list;
  }
}
