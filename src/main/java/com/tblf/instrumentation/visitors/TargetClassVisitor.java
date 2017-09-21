package com.tblf.instrumentation.visitors;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Created by Thibault on 20/09/2017.
 */
public class TargetClassVisitor extends ClassVisitor {
    public TargetClassVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        return new TargetMethodVisitor(this.api, methodVisitor, access, name, desc);
    }
}