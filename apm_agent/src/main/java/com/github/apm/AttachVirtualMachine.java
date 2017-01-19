package com.github.apm;

import java.io.File;

import com.sun.tools.attach.VirtualMachine;

public class AttachVirtualMachine {

  public static void main(String[] args) {
    String files =
        AttachVirtualMachine.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    System.out.println(files + "---");
    String processId = args[0];
    String path = System.getProperty("user.dir");
    System.out.println(path + "&&&");
    if (path.endsWith("/bin")) {
      path = path.replace("/bin", "");
    }
    String libPath = path + File.separator + "agent" + File.separator;
    String agentJar = "";
    String param = args.length >= 3 ? args[2] : "-path:" + path + ";";
    if (args.length >= 2) {
      if (args[1].startsWith("agent:")) {
        args[1] = args[1].substring(6);
        agentJar = args[1].startsWith("/") ? args[1] : libPath + args[1].startsWith("/");
      } else {
        param += args[1] + ";";
      }
    }
    if (agentJar.length() == 0) {
      agentJar = files;
    }
    VirtualMachine virtualMachine = null;
    try {
      virtualMachine = VirtualMachine.attach(processId);
      virtualMachine.loadAgent(agentJar, param);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("useage is java -jar apm_agent.jar $pid agent:$path \"$param\"");
    } finally {
      try {
        virtualMachine.detach();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
