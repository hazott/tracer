package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.isa.AMD64Instruction;
import org.graalvm.vm.x86.isa.ImmediateOperand;
import org.graalvm.vm.x86.isa.Operand;
import org.graalvm.vm.x86.isa.OperandDecoder;
import org.graalvm.vm.x86.node.ReadNode;
import org.graalvm.vm.x86.node.WriteFlagNode;
import org.graalvm.vm.x86.node.WriteNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Imul extends AMD64Instruction {
    protected final Operand operand1;
    protected final Operand operand2;
    protected final Operand operand3;

    @Child protected WriteFlagNode writeCF;
    @Child protected WriteFlagNode writeOF;

    protected Imul(long pc, byte[] instruction, Operand operand1) {
        this(pc, instruction, operand1, null, null);
    }

    protected Imul(long pc, byte[] instruction, Operand operand1, Operand operand2) {
        this(pc, instruction, operand1, operand2, null);
    }

    protected Imul(long pc, byte[] instruction, Operand operand1, Operand operand2, Operand operand3) {
        super(pc, instruction);
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    protected void createFlagNodes(ArchitecturalState state) {
        writeCF = state.getRegisters().getCF().createWrite();
        writeOF = state.getRegisters().getOF().createWrite();
    }

    public static abstract class Imul2 extends Imul {
        @Child protected ReadNode readOp1;
        @Child protected ReadNode readOp2;
        @Child protected WriteNode writeDst;

        private final Operand srcA;
        private final Operand srcB;
        private final Operand dst;

        protected Imul2(long pc, byte[] instruction, OperandDecoder operands, int type) {
            super(pc, instruction, operands.getOperand2(type), operands.getOperand1(type));
            this.dst = operand1;
            this.srcA = operand1;
            this.srcB = operand2;
        }

        protected Imul2(long pc, byte[] instruction, OperandDecoder operands, short imm, int type) {
            super(pc, instruction, operands.getOperand2(type), operands.getOperand1(type), new ImmediateOperand(imm));
            this.dst = operand1;
            this.srcA = operand2;
            this.srcB = operand3;
        }

        protected Imul2(long pc, byte[] instruction, OperandDecoder operands, int imm, int type) {
            super(pc, instruction, operands.getOperand2(type), operands.getOperand1(type), new ImmediateOperand(imm));
            this.dst = operand1;
            this.srcA = operand2;
            this.srcB = operand3;
        }

        protected Imul2(long pc, byte[] instruction, OperandDecoder operands, long imm, int type) {
            super(pc, instruction, operands.getOperand2(type), operands.getOperand1(type), new ImmediateOperand(imm));
            this.dst = operand1;
            this.srcA = operand2;
            this.srcB = operand3;
        }

        protected void createChildrenIfNecessary() {
            if (readOp1 == null) {
                CompilerDirectives.transferToInterpreter();
                ArchitecturalState state = getContextReference().get().getState();
                readOp1 = srcA.createRead(state, next());
                readOp2 = srcB.createRead(state, next());
                writeDst = dst.createWrite(state, next());
                createFlagNodes(state);
            }
        }
    }

    public static class Imulw extends Imul2 {
        public Imulw(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands, OperandDecoder.R16);
        }

        public Imulw(long pc, byte[] instruction, OperandDecoder operands, short imm) {
            super(pc, instruction, operands, imm, OperandDecoder.R16);
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            short op1 = readOp1.executeI16(frame);
            short op2 = readOp2.executeI16(frame);
            int result = op1 * op2;
            writeDst.executeI16(frame, (short) result);
            boolean overflow = result != (short) result;
            writeCF.execute(frame, overflow);
            writeOF.execute(frame, overflow);
            return next();
        }
    }

    public static class Imull extends Imul2 {
        public Imull(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands, OperandDecoder.R32);
        }

        public Imull(long pc, byte[] instruction, OperandDecoder operands, int imm) {
            super(pc, instruction, operands, imm, OperandDecoder.R32);
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            int op1 = readOp1.executeI32(frame);
            int op2 = readOp2.executeI32(frame);
            long result = (long) op1 * (long) op2;
            writeDst.executeI32(frame, (int) result);
            boolean overflow = result != (int) result;
            writeCF.execute(frame, overflow);
            writeOF.execute(frame, overflow);
            return next();
        }
    }

    public static class Imulq extends Imul2 {
        public Imulq(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands, OperandDecoder.R64);
        }

        public Imulq(long pc, byte[] instruction, OperandDecoder operands, long imm) {
            super(pc, instruction, operands, imm, OperandDecoder.R64);
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            long op1 = readOp1.executeI64(frame);
            long op2 = readOp2.executeI64(frame);
            long result = op1 * op2;
            writeDst.executeI64(frame, result);
            boolean overflow = op1 != 0 && (result / op1 != op2); // TODO: implement properly!
            writeCF.execute(frame, overflow);
            writeOF.execute(frame, overflow);
            return next();
        }
    }

    @Override
    protected String[] disassemble() {
        if (operand3 != null) {
            return new String[]{"imul", operand1.toString(), operand2.toString(), operand3.toString()};
        } else if (operand2 != null) {
            return new String[]{"imul", operand1.toString(), operand2.toString()};
        } else {
            return new String[]{"imul", operand1.toString()};
        }
    }
}
