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
package org.graalvm.vm.trcview.arch.x86.decode.isa.instruction;

import org.graalvm.vm.trcview.arch.x86.decode.isa.AMD64Instruction;
import org.graalvm.vm.trcview.arch.x86.decode.isa.Register;
import org.graalvm.vm.trcview.arch.x86.decode.isa.RegisterOperand;
import org.graalvm.vm.trcview.disasm.AssemblerInstruction;

public abstract class Cxe extends AMD64Instruction {
    protected static final int CBW = 0;
    protected static final int CWDE = 1;
    protected static final int CDQE = 2;

    private final int size;
    private final String name;

    protected Cxe(long pc, byte[] instruction, int size, String name) {
        super(pc, instruction);
        this.size = size;
        this.name = name;
        setGPRReadOperands(new RegisterOperand(Register.RAX));
        setGPRWriteOperands(new RegisterOperand(Register.RDX));
    }

    public static class Cbw extends Cxe {
        public Cbw(long pc, byte[] instruction) {
            super(pc, instruction, CBW, "cbw");
        }
    }

    public static class Cwde extends Cxe {
        public Cwde(long pc, byte[] instruction) {
            super(pc, instruction, CWDE, "cwde");
        }
    }

    public static class Cdqe extends Cxe {
        public Cdqe(long pc, byte[] instruction) {
            super(pc, instruction, CDQE, "cdqe");
        }
    }

    public int getOperandSize() {
        return size;
    }

    @Override
    protected AssemblerInstruction disassemble() {
        return new AssemblerInstruction(name);
    }
}
