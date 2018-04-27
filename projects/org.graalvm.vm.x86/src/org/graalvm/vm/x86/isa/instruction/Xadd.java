package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.isa.AMD64Instruction;
import org.graalvm.vm.x86.isa.Flags;
import org.graalvm.vm.x86.isa.Operand;
import org.graalvm.vm.x86.isa.OperandDecoder;
import org.graalvm.vm.x86.node.ReadNode;
import org.graalvm.vm.x86.node.WriteFlagNode;
import org.graalvm.vm.x86.node.WriteNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Xadd extends AMD64Instruction {
    private final Operand operand1;
    private final Operand operand2;

    @Child protected ReadNode srcA;
    @Child protected ReadNode srcB;
    @Child protected WriteNode src;
    @Child protected WriteNode dst;
    @Child protected WriteFlagNode writeCF;
    @Child protected WriteFlagNode writeOF;
    @Child protected WriteFlagNode writeSF;
    @Child protected WriteFlagNode writeZF;
    @Child protected WriteFlagNode writePF;

    protected void createChildrenIfNecessary() {
        if (srcA == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArchitecturalState state = getContextReference().get().getState();
            srcA = operand1.createRead(state, next());
            srcB = operand2.createRead(state, next());
            dst = operand1.createWrite(state, next());
            src = operand2.createWrite(state, next());
            writeCF = state.getRegisters().getCF().createWrite();
            writeOF = state.getRegisters().getOF().createWrite();
            writeSF = state.getRegisters().getSF().createWrite();
            writeZF = state.getRegisters().getZF().createWrite();
            writePF = state.getRegisters().getPF().createWrite();
        }
    }

    protected Xadd(long pc, byte[] instruction, Operand operand1, Operand operand2) {
        super(pc, instruction);
        this.operand1 = operand1;
        this.operand2 = operand2;

        setGPRReadOperands(operand1, operand2);
        setGPRWriteOperands(operand1, operand2);
    }

    public static class Xaddb extends Xadd {
        public Xaddb(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R8), operands.getOperand2(OperandDecoder.R8));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            byte a = srcA.executeI8(frame);
            byte b = srcB.executeI8(frame);
            byte result = (byte) (a + b);
            dst.executeI8(frame, result);
            src.executeI8(frame, a);

            boolean overflow = (result < 0 && a > 0 && b > 0) || (result >= 0 && a < 0 && b < 0);
            boolean carry = ((a < 0 || b < 0) && result >= 0) || (a < 0 && b < 0);
            writeCF.execute(frame, carry);
            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity(result));
            return next();
        }
    }

    public static class Xaddw extends Xadd {
        public Xaddw(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R16), operands.getOperand2(OperandDecoder.R16));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            short a = srcA.executeI16(frame);
            short b = srcB.executeI16(frame);
            short result = (short) (a + b);
            dst.executeI16(frame, result);
            src.executeI16(frame, a);

            boolean overflow = (result < 0 && a > 0 && b > 0) || (result >= 0 && a < 0 && b < 0);
            boolean carry = ((a < 0 || b < 0) && result >= 0) || (a < 0 && b < 0);
            writeCF.execute(frame, carry);
            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            return next();
        }
    }

    public static class Xaddl extends Xadd {
        public Xaddl(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R32), operands.getOperand2(OperandDecoder.R32));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            int a = srcA.executeI32(frame);
            int b = srcB.executeI32(frame);
            int result = a + b;
            dst.executeI32(frame, result);
            src.executeI32(frame, a);

            boolean overflow = (result < 0 && a > 0 && b > 0) || (result >= 0 && a < 0 && b < 0);
            boolean carry = ((a < 0 || b < 0) && result >= 0) || (a < 0 && b < 0);
            writeCF.execute(frame, carry);
            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            return next();
        }
    }

    public static class Xaddq extends Xadd {
        public Xaddq(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R64), operands.getOperand2(OperandDecoder.R64));
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            long a = srcA.executeI64(frame);
            long b = srcB.executeI64(frame);
            long result = a + b;
            dst.executeI64(frame, result);
            src.executeI64(frame, a);

            boolean overflow = (result < 0 && a > 0 && b > 0) || (result >= 0 && a < 0 && b < 0);
            boolean carry = ((a < 0 || b < 0) && result >= 0) || (a < 0 && b < 0);
            writeCF.execute(frame, carry);
            writeOF.execute(frame, overflow);
            writeSF.execute(frame, result < 0);
            writeZF.execute(frame, result == 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            return next();
        }
    }

    @Override
    protected String[] disassemble() {
        return new String[]{"xadd", operand1.toString(), operand2.toString()};
    }
}
