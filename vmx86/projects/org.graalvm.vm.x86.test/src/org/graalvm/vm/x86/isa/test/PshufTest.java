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
package org.graalvm.vm.x86.isa.test;

import org.graalvm.vm.x86.isa.instruction.Pshufb;
import org.graalvm.vm.x86.isa.instruction.Pshufd;
import org.graalvm.vm.x86.isa.instruction.Pshufhw;
import org.graalvm.vm.x86.isa.instruction.Pshuflw;
import org.graalvm.vm.x86.test.InstructionTest;
import org.junit.Test;

public class PshufTest extends InstructionTest {
    private static final byte[] MACHINECODE1 = {(byte) 0xf2, 0x0f, 0x70, (byte) 0xac, (byte) 0x8c, 0x10, 0x04, 0x00, 0x00, (byte) 0xe8};
    private static final String ASSEMBLY1 = "pshuflw\txmm5,[rsp+rcx*4+0x410],0xe8";

    private static final byte[] MACHINECODE2 = {0x66, 0x0f, 0x70, (byte) 0xf6, (byte) 0xe8};
    private static final String ASSEMBLY2 = "pshufd\txmm6,xmm6,0xe8";

    private static final byte[] MACHINECODE3 = {(byte) 0xf3, 0x0f, 0x70, (byte) 0xed, (byte) 0xe8};
    private static final String ASSEMBLY3 = "pshufhw\txmm5,xmm5,0xe8";

    private static final byte[] MACHINECODE4 = {0x66, 0x0f, 0x38, 0x00, (byte) 0xc3};
    private static final String ASSEMBLY4 = "pshufb\txmm0,xmm3";

    private static final byte[] MACHINECODE5 = {0x66, 0x0f, 0x38, 0x00, (byte) 0xf9};
    private static final String ASSEMBLY5 = "pshufb\txmm7,xmm1";

    @Test
    public void test1() {
        check(MACHINECODE1, ASSEMBLY1, Pshuflw.class);
    }

    @Test
    public void test2() {
        check(MACHINECODE2, ASSEMBLY2, Pshufd.class);
    }

    @Test
    public void test3() {
        check(MACHINECODE3, ASSEMBLY3, Pshufhw.class);
    }

    @Test
    public void test4() {
        check(MACHINECODE4, ASSEMBLY4, Pshufb.class);
    }

    @Test
    public void test5() {
        check(MACHINECODE5, ASSEMBLY5, Pshufb.class);
    }
}
