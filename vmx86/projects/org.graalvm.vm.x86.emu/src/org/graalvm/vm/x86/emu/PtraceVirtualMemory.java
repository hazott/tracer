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
package org.graalvm.vm.x86.emu;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.vm.memory.ByteMemory;
import org.graalvm.vm.memory.Memory;
import org.graalvm.vm.memory.MemoryPage;
import org.graalvm.vm.memory.MemorySegment;
import org.graalvm.vm.memory.PosixMemory;
import org.graalvm.vm.memory.VirtualMemory;
import org.graalvm.vm.memory.exception.SegmentationViolation;
import org.graalvm.vm.memory.hardware.NullMemory;
import org.graalvm.vm.memory.hardware.linux.MemoryMap;
import org.graalvm.vm.memory.vector.Vector128;
import org.graalvm.vm.memory.vector.Vector256;
import org.graalvm.vm.memory.vector.Vector512;
import org.graalvm.vm.posix.api.Errno;
import org.graalvm.vm.posix.api.PosixException;
import org.graalvm.vm.util.log.Levels;
import org.graalvm.vm.util.log.Trace;

public class PtraceVirtualMemory extends VirtualMemory {
    private static final Logger log = Trace.create(PtraceVirtualMemory.class);

    private Ptrace ptrace;

    public PtraceVirtualMemory(Ptrace ptrace) {
        this.ptrace = ptrace;
        setLE();
    }

    public boolean getDebug() {
        return enableAccessTrace;
    }

    public void setDebug(boolean debug) {
        enableAccessTrace = debug;
    }

    private static long a64(long addr) {
        return addr & 0xFFFFFFFFFFFFFFF8L;
    }

    private static long s64(long addr) {
        long a = a64(addr);
        return (addr - a) * 8;
    }

    @Override
    public byte getI8(long addr) {
        try {
            byte val = (byte) ptrace.read(addr);
            logMemoryRead(addr, 1, val);
            return val;
        } catch (PosixException e) {
            logMemoryRead(addr, 1);
            throw new SegmentationViolation(addr);
        }
    }

    @Override
    public void setI8(long addr, byte value) {
        logMemoryWrite(addr, 1, value);
        try {
            long a = a64(addr);
            long s = s64(addr);
            long m = 0x00000000000000FFL << s;

            long val = ptrace.read(a);
            val = (val & ~m) | (Byte.toUnsignedLong(value) << s);
            ptrace.write(a, val);
        } catch (PosixException e) {
            e.printStackTrace();
            throw new SegmentationViolation(addr);
        }
    }

    @Override
    public short getI16(long addr) {
        try {
            short val = (short) ptrace.read(addr);
            logMemoryRead(addr, 2, val);
            return val;
        } catch (PosixException e) {
            logMemoryRead(addr, 2);
            throw new SegmentationViolation(addr);
        }
    }

    @Override
    public void setI16(long addr, short value) {
        logMemoryWrite(addr, 2, value);
        try {
            long val = ptrace.read(addr);
            val = (val & ~0xFFFF) | Short.toUnsignedLong(value);
            ptrace.write(addr, val);
        } catch (PosixException e) {
            throw new SegmentationViolation(addr);
        }
    }

    @Override
    public int getI32(long addr) {
        try {
            int val = (int) ptrace.read(addr);
            logMemoryRead(addr, 4, val);
            return val;
        } catch (PosixException e) {
            logMemoryRead(addr, 4);
            throw new SegmentationViolation(addr);
        }
    }

    @Override
    public void setI32(long addr, int value) {
        logMemoryWrite(addr, 4, value);
        try {
            long val = ptrace.read(addr);
            val = (val & ~0xFFFFFFFFL) | Integer.toUnsignedLong(value);
            ptrace.write(addr, val);
        } catch (PosixException e) {
            throw new SegmentationViolation(addr);
        }
    }

    @Override
    public long getI64(long addr) {
        try {
            long val = ptrace.read(addr);
            logMemoryRead(addr, 8, val);
            return val;
        } catch (PosixException e) {
            logMemoryRead(addr, 8);
            throw new SegmentationViolation(addr);
        }
    }

    @Override
    public void setI64(long addr, long value) {
        logMemoryWrite(addr, 8, value);
        try {
            ptrace.write(addr, value);
        } catch (PosixException e) {
            throw new SegmentationViolation(addr);
        }
    }

    // TODO: this is *not* atomic for now!
    @Override
    public boolean cmpxchgI8(long address, byte expected, byte x) {
        setI8(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI16(long address, short expected, short x) {
        setI16(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI32(long address, int expected, int x) {
        setI32(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI64(long address, long expected, long x) {
        setI64(address, x);
        return true;
    }

    @Override
    public boolean cmpxchgI128(long address, Vector128 expected, Vector128 x) {
        setI128(address, x);
        return true;
    }

    @Override
    public void mprotect(long address, long len, boolean r, boolean w, boolean x) throws PosixException {
        ptrace.mprotect(address, len, r, w, x);
    }

    @Override
    public void add(MemoryPage page) {
        Memory mem = page.getMemory();
        if (mem instanceof PosixMemory) {
            assert ((PosixMemory) mem).isReadOnly();

            // copy
            long size = mem.size();
            Memory memory = new ByteMemory(size, false);
            long sz = mem.size();
            for (int i = 0; i < sz; i++) {
                memory.setI8(i, mem.getI8(i));
            }
            MemoryPage pag = new MemoryPage(memory, page.base, page.size, page.name, page.fileOffset);
            add(pag);
            return;
        } else if (!(mem instanceof ByteMemory) && !(mem instanceof NullMemory)) {
            throw new IllegalArgumentException("not a ByteMemory");
        }
        boolean ok = Long.compareUnsigned(page.end, pointerBase) <= 0 || Long.compareUnsigned(page.end, pointerEnd) > 0;
        if (!ok) {
            allocator.allocat(page.base, page.size);
        }
        long addr = addr(page.base);
        long start = pageStart(addr);
        long headsz = addr - start;
        long size = roundToPageSize(page.size + headsz);
        try {
            ptrace.mmap(start, size, page.r, true, page.x, true, true, false, -1, 0);
        } catch (PosixException e) {
            log.log(Levels.ERROR, "mmap failed: " + Errno.toString(e.getErrno()));
            throw new OutOfMemoryError("mmap failed: " + Errno.toString(e.getErrno()));
        }

        // System.out.printf("ADD: [0x%x-0x%x:0x%x] (0x%x-0x%x)\n", page.base, page.end, page.size,
        // start, start + size);

        // copy page content to native memory
        if (mem instanceof ByteMemory) {
            int i = 0;
            try {
                for (i = 0; i < page.size - 8; i += 8) {
                    long val = page.getI64(page.base + i);
                    setI64(page.base + i, val);
                }
            } catch (SegmentationViolation e) {
                // ignore
            }
            try {
                for (; i < page.size; i++) {
                    byte val = page.getI8(page.base + i);
                    setI8(page.base + i, val);
                }
            } catch (SegmentationViolation e) {
                // ignore
            }
        }

        try {
            ptrace.mprotect(start, size, page.r, page.w, page.x);
        } catch (PosixException e) {
            log.log(Levels.ERROR, "mprotect failed: " + Errno.toString(e.getErrno()));
            throw new OutOfMemoryError("mprotect failed: " + Errno.toString(e.getErrno()));
        }
    }

    @Override
    public void remove(long address, long len) throws PosixException {
        long addr = addr(address);
        allocator.free(address, len);
        ptrace.munmap(addr, len);
    }

    @Override
    public MemoryPage allocate(long size, String name) {
        long base = allocateRegion(size);
        MemoryPage page = new MemoryPage(new NullMemory(bigEndian, size), base, size, name);
        add(page);
        return page;
    }

    @Override
    public MemoryPage allocate(Memory memory, long size, String name, long offset) {
        if (memory instanceof PosixMemory) {
            assert ((PosixMemory) memory).isReadOnly();

            // copy
            Memory mem = new ByteMemory(size, false);
            long sz = memory.size();
            for (int i = 0; i < sz; i++) {
                mem.setI8(i, memory.getI8(i));
            }
            return allocate(mem, size, name, offset);
        } else if (!(memory instanceof ByteMemory)) {
            throw new IllegalArgumentException("not a ByteMemory");
        }

        long base = allocateRegion(size);
        MemoryPage page = new MemoryPage(memory, base, size);
        add(page);
        return page;
    }

    @Override
    public void free(long address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(long address) {
        return true;
    }

    @Override
    public Vector128 getI128(long address) {
        if (bigEndian) {
            long hi = getI64(address);
            long lo = getI64(address + 8);
            return new Vector128(hi, lo);
        } else {
            long lo = getI64(address);
            long hi = getI64(address + 8);
            return new Vector128(hi, lo);
        }
    }

    @Override
    public Vector256 getI256(long address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Vector512 getI512(long address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setI128(long address, Vector128 val) {
        if (bigEndian) {
            setI64(address, val.getI64(0));
            setI64(address + 8, val.getI64(1));
        } else {
            setI64(address, val.getI64(1));
            setI64(address + 8, val.getI64(0));
        }
    }

    @Override
    public void setI128(long address, long hi, long lo) {
        if (bigEndian) {
            setI64(address, hi);
            setI64(address + 8, lo);
        } else {
            setI64(address, lo);
            setI64(address + 8, hi);
        }
    }

    @Override
    public void setI256(long address, Vector256 val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setI512(long address, Vector512 val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExecutable(long address) {
        return true; // TODO: not correct but also not too harmful
    }

    @Override
    public void printMaps(PrintStream out) {
        try {
            MemoryMap map = new MemoryMap(ptrace.getPid());
            for (MemorySegment segment : map.getSegments()) {
                out.println(segment);
            }
        } catch (IOException e) {
            out.println("Error while retrieving memory map:");
            e.printStackTrace(out);
        }
    }

    @Override
    public void printAddressInfo(long addr, PrintStream out) {
        try {
            MemoryMap map = new MemoryMap(ptrace.getPid());
            for (MemorySegment s : map.getSegments()) {
                if (s.contains(addr(addr))) {
                    out.printf("Memory region name: '%s', base = 0x%016x (offset = 0x%016x)\n", s.name, s.start, addr - s.start);
                }
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Cannot retrieve memory region info", e);
        }
    }

    @Override
    public List<MemorySegment> getSegments() {
        try {
            MemoryMap map = new MemoryMap(ptrace.getPid());
            return map.getSegments();
        } catch (IOException e) {
            log.log(Level.WARNING, "Cannot retrieve memory map", e);
            return Collections.emptyList();
        }
    }
}
