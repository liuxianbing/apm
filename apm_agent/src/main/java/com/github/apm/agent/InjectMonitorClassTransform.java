package com.github.apm.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class InjectMonitorClassTransform implements ClassFileTransformer {
  public InjectMonitorClassTransform() {}

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer)
      throws IllegalClassFormatException {
    // 简化测试demo，直接写待修改的类(com/blueware/agent/TestTime)
    if (className != null
        && className.contains("com/jingoal/dc/koala/web/controller/LoginController")) {
      // 读取类的字节码流
      ClassReader reader = new ClassReader(classfileBuffer);
      // 创建操作字节流值对象，ClassWriter.COMPUTE_MAXS:表示自动计算栈大小
      ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
      // 接受一个ClassVisitor子类进行字节码修改
      reader.accept(new TimeClassVisitor(writer, className), 8);
      // 返回修改后的字节码流
      return writer.toByteArray();
    } else {
      return null;
    }
  }

}
