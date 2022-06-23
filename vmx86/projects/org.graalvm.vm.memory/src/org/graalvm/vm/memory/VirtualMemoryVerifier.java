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
package org.graalvm.vm.memory;

import java.io.PrintStream;
import java.util.Collection;
import java.util.logging.Logger;

import org.graalvm.vm.memory.hardware.HybridVirtualMemory;
import org.graalvm.vm.memory.vector.Vector128;
import org.graalvm.vm.memory.vector.Vector256;
import org.graalvm.vm.memory.vector.Vector512;
import org.graalvm.vm.posix.api.PosixException;
import org.graalvm.vm.util.HexFormatter;
import org.graalvm.vm.util.log.Levels;
import org.graalvm.vm.util.log.Trace;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;

public class VirtualMemoryVerifier extends VirtualMemory {
    private static final Logger log = Trace.create(VirtualMemoryVerifier.class);

    private JavaVirtualMemory jmem;
    private HybridVirtualMemory nmem;

    public VirtualMemoryVerifier() {
        jmem = new JavaVirtualMemory();
        nmem = new HybridVirtualMemory();
    }

    @Override
    public long getNativeAddress(long addr) {
        return nmem.getNativeAddress(addr);
    }

    @Override
    public void setAccessLogger(MemoryAccessListener logger) {
        super.setAccessLogger(logger);
        jmem.setAccessLogger(logger);
    }

    @Override
    public void add(MemoryPage page) {
        CompilerAsserts.neverPartOfCompilation();
        jmem.add(page);
        nmem.add(page);
    }

    @Override
    public void remove(long addr, long len) throws PosixException {
        CompilerAsserts.neverPartOfCompilation();
        jmem.remove(addr, len);
        nmem.remove(addr, len);
    }

    @Override
    public MemoryPage allocate(long size, String name) {
        CompilerAsserts.neverPartOfCompilation();
        MemoryPage page = nmem.allocate(size, name);
        Memory mem = new ByteMemory(new byte[(int) page.size], false);
        MemoryPage pag = new MemoryPage(mem, page.base, page.size, page.name, page.fileOffset);
        jmem.add(pag);
        return pag;
    }

    @Override
    public MemoryPage allocate(Memory memory, long size, String name, long offset) {
        CompilerAsserts.neverPartOfCompilation();
        MemoryPage page = nmem.allocate(memory, size, name, offset);
        jmem.add(page);
        return page;
    }

    @Override
    public void free(long address) {
        CompilerAsserts.neverPartOfCompilation();
        jmem.free(address);
        nmem.free(address);
    }

    @Override
    public boolean contains(long address) {
        boolean c1 = jmem.contains(address);
        // don't check for behavior differences here since native memory does not provide this
        // information

        // boolean c2 = nmem.contains(address);
        // if (c1 != c2) {
        // CompilerDirectives.transferToInterpreter();
        // log.warning("Behavior mismatch at " + HexFormatter.tohex(address, 16) + ": " + c1 + " vs
        // " + c2);
        // }
        return c1;
    }

    private static final void fail(long addr, String msg) {
        CompilerDirectives.transferToInterpreter();
        log.log(Levels.ERROR, "Data mismatch at " + HexFormatter.tohex(addr, 16) + ": " + msg);
    }

    private static final void fail(long addr, byte b1, byte b2) {
        CompilerDirectives.transferToInterpreter();
        fail(addr, "0x" + HexFormatter.tohex(b1 & 0xFF, 2) + " vs 0x" + HexFormatter.tohex(b2 & 0xFF, 2));
    }

    private static final void fail(long addr, short s1, short s2) {
        CompilerDirectives.transferToInterpreter();
        fail(addr, "0x" + HexFormatter.tohex(s1 & 0xFFFF, 4) + " vs 0x" + HexFormatter.tohex(s2 & 0xFFFF, 4));
    }

    private static final void fail(long addr, int i1, int i2) {
        CompilerDirectives.transferToInterpreter();
        fail(addr, "0x" + HexFormatter.tohex(i1, 8) + " vs 0x" + HexFormatter.tohex(i2, 8));
    }

    private static final void fail(long addr, long l1, long l2) {
        CompilerDirectives.transferToInterpreter();
        fail(addr, "0x" + HexFormatter.tohex(l1, 16) + " vs 0x" + HexFormatter.tohex(l2, 16));
    }

    @Override
    public byte getI8(long address) {
        byte b1 = jmem.getI8(address);
        byte b2 = nmem.getI8(address);
        if (b1 != b2) {
            fail(address, b1, b2);
        }
        return b1;
    }

    @Override
    public short getI16(long address) {
        short s1 = jmem.getI16(address);
        short s2 = nmem.getI16(address);
        if (s1 != s2) {
            fail(address, s1, s2);
        }
        return s1;
    }

    @Override
    public int getI32(long address) {
        int i1 = jmem.getI32(address);
        int i2 = nmem.getI32(address);
        if (i1 != i2) {
            fail(address, i1, i2);
        }
        return i1;
    }

    @Override
    public long getI64(long address) {
        long l1 = jmem.getI64(address);
        long l2 = nmem.getI64(address);
        if (l1 != l2) {
            fail(address, l1, l2);
        }
        return l1;
    }

    @Override
    public Vector128 getI128(long address) {
        Vector128 v1 = jmem.getI128(address);
        Vector128 v2 = nmem.getI128(address);
        if (!v1.equals(v2)) {
            CompilerDirectives.transferToInterpreter();
            fail(address, v1 + " vs " + v2);
        }
        return v1;
    }

    @Override
    public Vector256 getI256(long address) {
        Vector256 v1 = jmem.getI256(address);
        Vector256 v2 = nmem.getI256(address);
        if (!v1.equals(v2)) {
            CompilerDirectives.transferToInterpreter();
            fail(address, v1 + " vs " + v2);
        }
        return v1;
    }

    @Override
    public Vector512 getI512(long address) {
        Vector512 v1 = jmem.getI512(address);
        Vector512 v2 = nmem.getI512(address);
        if (!v1.equals(v2)) {
            CompilerDirectives.transferToInterpreter();
            fail(address, v1 + " vs " + v2);
        }
        return v1;
    }

    @Override
    public void setI8(long address, byte val) {
        jmem.setI8(address, val);
        nmem.setI8(address, val);
    }

    @Override
    public void setI16(long address, short val) {
        jmem.setI16(address, val);
        nmem.setI16(address, val);
    }

    @Override
    public void setI32(long address, int val) {
        jmem.setI32(address, val);
        nmem.setI32(address, val);
    }

    @Override
    public void setI64(long address, long val) {
        jmem.setI64(address, val);
        nmem.setI64(address, val);
    }

    @Override
    public void setI128(long address, Vector128 val) {
        jmem.setI128(address, val);
        nmem.setI128(address, val);
    }

    @Override
    public void setI128(long address, long hi, long lo) {
        jmem.setI128(address, hi, lo);
        nmem.setI128(address, hi, lo);
    }

    @Override
    public void setI256(long address, Vector256 val) {
        jmem.setI256(address, val);
        nmem.setI256(address, val);
    }

    @Override
    public void setI512(long address, Vector512 val) {
        jmem.setI512(address, val);
        nmem.setI512(address, val);
    }

    // VirtualMemoryVerifier is single threaded only!
    @Override
    public boolean cmpxchgI8(long address, byte expected, byte x) {
        jmem.setI8(address, x);
        nmem.setI8(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI16(long address, short expected, short x) {
        jmem.setI16(address, x);
        nmem.setI16(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI32(long address, int expected, int x) {
        jmem.setI32(address, x);
        nmem.setI32(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI64(long address, long expected, long x) {
        jmem.setI64(address, x);
        nmem.setI64(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI128(long address, Vector128 expected, Vector128 x) {
        jmem.setI128(address, x);
        nmem.setI128(address, x);
        return true;
    }

    @Override
    public void mprotect(long address, long len, boolean r, boolean w, boolean x) throws PosixException {
        jmem.mprotect(address, len, r, w, x);
        nmem.mprotect(address, len, r, w, x);
    }

    @Override
    public boolean isExecutable(long address) {
        boolean e1 = jmem.isExecutable(address);
        boolean e2 = nmem.isExecutable(address);
        if (e1 != e2) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError("Behavior mismatch");
        }
        return e1;
    }

    @Override
    public void printMaps(PrintStream out) {
        CompilerAsserts.neverPartOfCompilation();
        jmem.printMaps(out);
    }

    @Override
    public void printAddressInfo(long address, PrintStream out) {
        CompilerAsserts.neverPartOfCompilation();
        jmem.printAddressInfo(address, out);
    }

    @Override
    public Collection<MemorySegment> getSegments() {
        CompilerAsserts.neverPartOfCompilation();
        return jmem.getSegments();
    }
}
