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
package org.graalvm.vm.x86.isa;

import java.util.HashMap;
import java.util.Map;

import org.graalvm.vm.posix.elf.Elf;
import org.graalvm.vm.posix.elf.ProgramHeader;
import org.graalvm.vm.posix.elf.Symbol;

public class AMD64Disassembler {
    public static String disassemble(Elf elf) {
        Map<Long, Symbol> symbols = new HashMap<>();
        for (Symbol sym : elf.getSymbolTable().getSymbols()) {
            if (sym.getSectionIndex() != Symbol.SHN_UNDEF) {
                symbols.put(sym.getValue(), sym);
            }
        }
        StringBuilder buf = new StringBuilder();
        for (ProgramHeader hdr : elf.getProgramHeaders()) {
            if (hdr.getType() == Elf.PT_LOAD) {
                if (!hdr.getFlag(Elf.PF_X)) {
                    // not executable
                    continue;
                }
                assert (int) hdr.getMemorySize() == hdr.getMemorySize();
                CodeSegmentReader reader = new CodeSegmentReader(hdr);
                while (reader.isAvailable()) {
                    long pc = reader.getPC();
                    AMD64Instruction insn = AMD64InstructionDecoder.decode(pc, reader);
                    Symbol sym = symbols.get(pc);
                    if (sym != null) {
                        buf.append(sym.getName()).append(":\n");
                    }
                    buf.append(String.format("%016x: %s\n", pc, insn));
                }
            }
        }
        return buf.toString();
    }
}
