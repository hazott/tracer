package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.x86.isa.AMD64Instruction;

import com.oracle.truffle.api.frame.VirtualFrame;

public class Endbr64 extends AMD64Instruction {
    public Endbr64(long pc, byte[] instruction) {
        super(pc, instruction);
    }

    @Override
    public long executeInstruction(VirtualFrame frame) {
        return next();
    }

    @Override
    protected String[] disassemble() {
        return new String[]{"endbr64"};
    }
}