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
package org.graalvm.vm.util.io;

import java.io.IOException;
import java.io.InputStream;

public class BEInputStream extends WordInputStream {
    private boolean debug = false;

    public BEInputStream(InputStream parent) {
        super(parent);
    }

    public BEInputStream(InputStream parent, long offset) {
        super(parent, offset);
    }

    @Override
    public int read8bit() throws IOException {
        if (debug) {
            int r = read();
            System.out.println("u8: " + r + " (s8: " + (byte) r + "; bin: " + Integer.toString(r, 2) + ")");
            return r;
        }
        return read();
    }

    @Override
    public short read16bit() throws IOException {
        byte[] buf = new byte[2];
        read(buf);
        if (debug) {
            short r = Endianess.get16bitBE(buf);
            System.out.println("u16: " + Short.toUnsignedInt(r) + " (s16: " + r + "; bin: " + Integer.toString(Short.toUnsignedInt(r), 2) + ")");
            return r;
        }
        return Endianess.get16bitBE(buf);
    }

    @Override
    public int read24bit() throws IOException {
        byte[] buf = new byte[3];
        read(buf);
        if (debug) {
            int r = Endianess.get24bitBE(buf);
            System.out.println("u24: " + Integer.toUnsignedString(r) + " (s24: " + (r << 8 >> 8) + "; bin: " + Integer.toUnsignedString(r, 2) + ")");
            return r;
        }
        return Endianess.get24bitBE(buf);
    }

    @Override
    public int read32bit() throws IOException {
        byte[] buf = new byte[4];
        read(buf);
        if (debug) {
            int r = Endianess.get32bitBE(buf);
            System.out.println("u32: " + Integer.toUnsignedString(r) + " (s32: " + r + "; bin: " + Integer.toUnsignedString(r, 2) + ")");
            return r;
        }
        return Endianess.get32bitBE(buf);
    }

    @Override
    public long read64bit() throws IOException {
        byte[] buf = new byte[8];
        read(buf);
        if (debug) {
            long r = Endianess.get64bitBE(buf);
            System.out.println("u64: " + Long.toUnsignedString(r) + " (s64: " + r + "; bin: " + Long.toUnsignedString(r, 2) + ")");
            return r;
        }
        return Endianess.get64bitBE(buf);
    }

    public void debug() {
        this.debug = true;
    }

    public boolean isDebug() {
        return debug;
    }
}
