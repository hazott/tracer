/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graalvm.vm.x86.isa.instruction;

import org.graalvm.vm.math.LongDivision;
import org.graalvm.vm.math.LongDivision.Result;
import org.graalvm.vm.x86.ArchitecturalState;
import org.graalvm.vm.x86.isa.AMD64Instruction;
import org.graalvm.vm.x86.isa.Operand;
import org.graalvm.vm.x86.isa.OperandDecoder;
import org.graalvm.vm.x86.isa.Register;
import org.graalvm.vm.x86.isa.RegisterOperand;
import org.graalvm.vm.x86.node.ReadNode;
import org.graalvm.vm.x86.node.WriteNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Div extends AMD64Instruction {
    private static final String DIV_ZERO = "Division by zero";
    private static final String DIV_RANGE = "Integer overflow";

    protected final Operand operand;

    @Child protected ReadNode readOp;

    protected Div(long pc, byte[] instruction, Operand operand) {
        super(pc, instruction);
        this.operand = operand;
    }

    public static class Divb extends Div {
        @Child private ReadNode readAX;
        @Child private WriteNode writeAX;

        public Divb(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R8));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX));
            setGPRWriteOperands(operand, new RegisterOperand(Register.RAX));
        }

        @Override
        protected void createChildNodes() {
            ArchitecturalState state = getState();
            readAX = state.getRegisters().getRegister(Register.AX).createRead();
            readOp = operand.createRead(state, next());
            writeAX = state.getRegisters().getRegister(Register.AX).createWrite();
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            int ax = Short.toUnsignedInt(readAX.executeI16(frame));
            int op = Byte.toUnsignedInt(readOp.executeI8(frame));
            if (op == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new ArithmeticException(DIV_ZERO); // TODO: #DE
            }
            int q = ax / op;
            int r = ax % op;
            if (q > 0xFF) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException(DIV_RANGE); // TODO: #DE
            }
            short result = (short) ((q & 0xFF) | ((r & 0xFF) << 8));
            writeAX.executeI16(frame, result);
            return next();
        }
    }

    public static class Divw extends Div {
        @Child private ReadNode readAX;
        @Child private ReadNode readDX;
        @Child private WriteNode writeAX;
        @Child private WriteNode writeDX;

        public Divw(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R16));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
            setGPRWriteOperands(operand, new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
        }

        @Override
        protected void createChildNodes() {
            ArchitecturalState state = getState();
            readAX = state.getRegisters().getRegister(Register.AX).createRead();
            readDX = state.getRegisters().getRegister(Register.DX).createRead();
            readOp = operand.createRead(state, next());
            writeAX = state.getRegisters().getRegister(Register.AX).createWrite();
            writeDX = state.getRegisters().getRegister(Register.DX).createWrite();
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            short ax = readAX.executeI16(frame);
            short dx = readDX.executeI16(frame);
            int input = Short.toUnsignedInt(ax) | (Short.toUnsignedInt(dx) << 16);
            int op = Short.toUnsignedInt(readOp.executeI16(frame));
            if (op == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new ArithmeticException(DIV_ZERO); // TODO: #DE
            }
            int q = Integer.divideUnsigned(input, op);
            int r = Integer.remainderUnsigned(input, op);
            if (q > 0xFFFF) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException(DIV_RANGE); // TODO: #DE
            }
            writeAX.executeI16(frame, (short) q);
            writeDX.executeI16(frame, (short) r);
            return next();
        }
    }

    public static class Divl extends Div {
        @Child private ReadNode readEAX;
        @Child private ReadNode readEDX;
        @Child private WriteNode writeEAX;
        @Child private WriteNode writeEDX;

        public Divl(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R32));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
            setGPRWriteOperands(operand, new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
        }

        @Override
        protected void createChildNodes() {
            ArchitecturalState state = getState();
            readEAX = state.getRegisters().getRegister(Register.EAX).createRead();
            readEDX = state.getRegisters().getRegister(Register.EDX).createRead();
            readOp = operand.createRead(state, next());
            writeEAX = state.getRegisters().getRegister(Register.EAX).createWrite();
            writeEDX = state.getRegisters().getRegister(Register.EDX).createWrite();
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            int eax = readEAX.executeI32(frame);
            int edx = readEDX.executeI32(frame);
            long input = Integer.toUnsignedLong(eax) | (Integer.toUnsignedLong(edx) << 32);
            long op = Integer.toUnsignedLong(readOp.executeI32(frame));
            if (op == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new ArithmeticException(DIV_ZERO); // TODO: #DE
            }
            long q = Long.divideUnsigned(input, op);
            long r = Long.remainderUnsigned(input, op);
            if (q > 0xFFFFFFFFL) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException(DIV_RANGE); // TODO: #DE
            }
            writeEAX.executeI32(frame, (int) q);
            writeEDX.executeI32(frame, (int) r);
            return next();
        }
    }

    public static class Divq extends Div {
        @Child private ReadNode readRAX;
        @Child private ReadNode readRDX;
        @Child private WriteNode writeRAX;
        @Child private WriteNode writeRDX;

        public Divq(long pc, byte[] instruction, OperandDecoder operands) {
            super(pc, instruction, operands.getOperand1(OperandDecoder.R64));

            setGPRReadOperands(operand, new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
            setGPRWriteOperands(operand, new RegisterOperand(Register.RAX), new RegisterOperand(Register.RDX));
        }

        @Override
        protected void createChildNodes() {
            ArchitecturalState state = getState();
            readRAX = state.getRegisters().getRegister(Register.RAX).createRead();
            readRDX = state.getRegisters().getRegister(Register.RDX).createRead();
            readOp = operand.createRead(state, next());
            writeRAX = state.getRegisters().getRegister(Register.RAX).createWrite();
            writeRDX = state.getRegisters().getRegister(Register.RDX).createWrite();
        }

        @TruffleBoundary
        private static Result divu128by64(long a1, long a0, long b) {
            return LongDivision.divu128by64(a1, a0, b);
        }

        @Override
        public long executeInstruction(VirtualFrame frame) {
            long rax = readRAX.executeI64(frame);
            long rdx = readRDX.executeI64(frame);
            long op = readOp.executeI64(frame);
            if (op == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new ArithmeticException(DIV_ZERO); // TODO: #DE
            }
            long q;
            long r;
            if (rdx != 0) {
                Result result = divu128by64(rdx, rax, op);
                if (result.isInvalid()) {
                    CompilerDirectives.transferToInterpreter();
                    throw new ArithmeticException(DIV_RANGE); // TODO: #DE
                }
                q = result.quotient;
                r = result.remainder;
            } else {
                q = Long.divideUnsigned(rax, op);
                r = Long.remainderUnsigned(rax, op);
            }
            writeRAX.executeI64(frame, q);
            writeRDX.executeI64(frame, r);
            return next();
        }
    }

    @Override
    protected String[] disassemble() {
        return new String[]{"div", operand.toString()};
    }
}
