package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.RegisterAccessFactory;
import org.graalvm.vm.x86.isa.AMD64Instruction;
import org.graalvm.vm.x86.isa.Flags;
import org.graalvm.vm.x86.isa.Operand;
import org.graalvm.vm.x86.isa.OperandDecoder;
import org.graalvm.vm.x86.isa.Register;
import org.graalvm.vm.x86.isa.RegisterOperand;
import org.graalvm.vm.x86.node.ReadNode;
import org.graalvm.vm.x86.node.WriteFlagNode;
import org.graalvm.vm.x86.node.WriteNode;

import com.everyware.math.LongMultiplication;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Mul extends AMD64Instruction {
    protected final Operand operand;

    @Child protected WriteFlagNode writeCF;
    @Child protected WriteFlagNode writeOF;
    @Child protected WriteFlagNode writeSF;
    @Child protected WriteFlagNode writePF;

    protected Mul(long pc, byte[] instruction, Operand operand) {
        super(pc, instruction);
        this.operand = operand;
    }

    protected void createWriteFlagNodesIfNecessary() {
        if (writeCF == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            ArchitecturalState state = getContextReference().get().getState();
            RegisterAccessFactory regs = state.getRegisters();
            writeCF = regs.getCF().createWrite();
            writeOF = regs.getOF().createWrite();
            writeSF = regs.getSF().createWrite();
            writePF = regs.getPF().createWrite();
        }
    }

    public static class Mulb extends Mul {
        @Child private ReadNode readAL;
        @Child private ReadNode readOp;
        @Child private WriteNode writeAX;

        public Mulb(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R8));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX));
            setGPRWriteOperands(new RegisterOperand(Register.RAX));
        }

        private void createChildrenIfNecessary() {
            if (readAL == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createWriteFlagNodesIfNecessary();
                ArchitecturalState state = getContextReference().get().getState();
                readAL = state.getRegisters().getRegister(Register.AL).createRead();
                readOp = operand.createRead(state, next());
                writeAX = state.getRegisters().getRegister(Register.AX).createWrite();
            }
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            byte al = readAL.executeI8(frame);
            byte op = readOp.executeI8(frame);
            int result = Byte.toUnsignedInt(al) * Byte.toUnsignedInt(op);
            writeAX.executeI16(frame, (short) result);
            boolean of = (byte) (result >> 8) != 0;
            writeCF.execute(frame, of);
            writeOF.execute(frame, of);
            writeSF.execute(frame, (byte) result < 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            return next();
        }
    }

    public static class Mulw extends Mul {
        @Child private ReadNode readAX;
        @Child private ReadNode readOp;
        @Child private WriteNode writeAX;
        @Child private WriteNode writeDX;

        public Mulw(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R16));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX));
            setGPRWriteOperands(new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
        }

        private void createChildrenIfNecessary() {
            if (readAX == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createWriteFlagNodesIfNecessary();
                ArchitecturalState state = getContextReference().get().getState();
                readAX = state.getRegisters().getRegister(Register.AX).createRead();
                readOp = operand.createRead(state, next());
                writeAX = state.getRegisters().getRegister(Register.AX).createWrite();
                writeDX = state.getRegisters().getRegister(Register.DX).createWrite();
            }
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            short ax = readAX.executeI16(frame);
            short op = readOp.executeI16(frame);
            int result = Short.toUnsignedInt(ax) * Short.toUnsignedInt(op);
            short resultL = (short) result;
            short resultH = (short) (result >> 16);
            writeAX.executeI16(frame, resultL);
            writeDX.executeI16(frame, resultH);
            boolean of = resultH != 0;
            writeCF.execute(frame, of);
            writeOF.execute(frame, of);
            writeSF.execute(frame, resultL < 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            return next();
        }
    }

    public static class Mull extends Mul {
        @Child private ReadNode readEAX;
        @Child private ReadNode readOp;
        @Child private WriteNode writeEAX;
        @Child private WriteNode writeEDX;

        public Mull(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R32));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX));
            setGPRWriteOperands(new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
        }

        private void createChildrenIfNecessary() {
            if (readEAX == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createWriteFlagNodesIfNecessary();
                ArchitecturalState state = getContextReference().get().getState();
                readEAX = state.getRegisters().getRegister(Register.EAX).createRead();
                readOp = operand.createRead(state, next());
                writeEAX = state.getRegisters().getRegister(Register.EAX).createWrite();
                writeEDX = state.getRegisters().getRegister(Register.EDX).createWrite();
            }
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            int eax = readEAX.executeI32(frame);
            int op = readOp.executeI32(frame);
            long result = Integer.toUnsignedLong(eax) * Integer.toUnsignedLong(op);
            int resultL = (int) result;
            int resultH = (int) (result >> 32);
            writeEAX.executeI32(frame, resultL);
            writeEDX.executeI32(frame, resultH);
            boolean of = resultH != 0;
            writeCF.execute(frame, of);
            writeOF.execute(frame, of);
            writeSF.execute(frame, resultL < 0);
            writePF.execute(frame, Flags.getParity((byte) result));
            return next();
        }
    }

    public static class Mulq extends Mul {
        @Child private ReadNode readRAX;
        @Child private ReadNode readOp;
        @Child private WriteNode writeRAX;
        @Child private WriteNode writeRDX;

        public Mulq(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R64));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX));
            setGPRWriteOperands(new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
        }

        private void createChildrenIfNecessary() {
            if (readRAX == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                createWriteFlagNodesIfNecessary();
                ArchitecturalState state = getContextReference().get().getState();
                readRAX = state.getRegisters().getRegister(Register.RAX).createRead();
                readOp = operand.createRead(state, next());
                writeRAX = state.getRegisters().getRegister(Register.RAX).createWrite();
                writeRDX = state.getRegisters().getRegister(Register.RDX).createWrite();
            }
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            createChildrenIfNecessary();
            long rax = readRAX.executeI64(frame);
            long op = readOp.executeI64(frame);
            long resultL = rax * op;
            long resultH = LongMultiplication.multiplyHighUnsigned(rax, op);
            writeRAX.executeI64(frame, resultL);
            writeRDX.executeI64(frame, resultH);
            boolean of = resultH != 0;
            writeCF.execute(frame, of);
            writeOF.execute(frame, of);
            writeSF.execute(frame, resultL < 0);
            writePF.execute(frame, Flags.getParity((byte) resultL));
            return next();
        }
    }

    @Override
    protected String[] disassemble() {
        return new String[]{"mul", operand.toString()};
    }
}
