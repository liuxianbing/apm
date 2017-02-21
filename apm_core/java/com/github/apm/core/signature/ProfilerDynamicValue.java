// package com.github.apm.core.signature;
//
// import com.github.apm.core.transform.ApmMonitorByteBuddyTransformer.ApmmonitorDynamicValue;
//
// import net.bytebuddy.description.annotation.AnnotationDescription.Loadable;
// import net.bytebuddy.description.method.MethodDescription;
// import net.bytebuddy.description.method.ParameterDescription.InDefinedShape;
// import net.bytebuddy.description.type.TypeDescription;
// import net.bytebuddy.implementation.bytecode.assign.Assigner;
//
// public class ProfilerDynamicValue extends ApmmonitorDynamicValue<ProfilerSignature> {
//
// @Override
// public Class<ProfilerSignature> getAnnotationClass() {
// return ProfilerSignature.class;
// }
//
// @Override
// protected Object doResolve(TypeDescription instrumentedType, MethodDescription
// instrumentedMethod,
// InDefinedShape target, Loadable<ProfilerSignature> annotation, Assigner assigner,
// boolean initialized) {
// return "fff";
// }
//
// //
// // @Override
// // public Class<ProfilerSignature> getAnnotationClass() {
// // return ProfilerSignature.class;
// // }
// //
// // // protected Object doResolve(TypeDescription instrumentedType, MethodDescription. method,
// // // ParameterDescription.InDefinedShape target,
// // // AnnotationDescription.Loadable<ProfilerSignature> annotation, Assigner assigner,
// // // boolean initialized) {
// // @Override
// // public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
// // ParameterDescription.InDefinedShape target,
// // AnnotationDescription.Loadable<ProfilerSignature> annotation, boolean initialized) {
// // // final String returnType = method.getReturnType().asErasure().getSimpleName();
// // // final String className = method.getDeclaringType().getTypeName();
// // // String[] resList = new String[4];
// // // resList[0] = className;
// // // resList[1] = method.getName();
// // // resList[2] = returnType;
// // // resList[3] = getSignature(method);
// // // return resList;
// // return "abc";
// // // return String.format("%s %s.%s(%s)", returnType, className, method.getName(),
// // // getSignature(method));
// // }
// //
// // public String getSignature(MethodDescription.InDefinedShape instrumentedMethod) {
// // StringBuilder stringBuilder = new StringBuilder();
// // boolean comma = false;
// // for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList()
// // .asErasures()) {
// // if (comma) {
// // stringBuilder.append(',');
// // } else {
// // comma = true;
// // }
// // stringBuilder.append(typeDescription.getSimpleName());
// // }
// // return stringBuilder.toString();
// // }
//
// }
