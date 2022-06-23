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
package org.graalvm.vm.posix.vfs;

import static org.graalvm.vm.posix.api.io.Fcntl.O_RDONLY;
import static org.graalvm.vm.posix.api.io.Fcntl.O_RDWR;
import static org.graalvm.vm.posix.api.io.Fcntl.O_TMPFILE;
import static org.graalvm.vm.posix.api.io.Fcntl.O_WRONLY;

import org.graalvm.vm.posix.api.Errno;
import org.graalvm.vm.posix.api.PosixException;
import org.graalvm.vm.posix.api.io.Stat;
import org.graalvm.vm.posix.api.io.Statx;
import org.graalvm.vm.posix.api.io.Stream;
import org.graalvm.vm.util.BitTest;

public abstract class VFSFile extends VFSEntry {
    public VFSFile(VFSDirectory parent, String path, long uid, long gid, long permissions) {
        super(parent, path, uid, gid, permissions);
    }

    @SuppressWarnings("unused")
    protected Stream open(boolean read, boolean write) throws PosixException {
        throw new AssertionError("not implemented");
    }

    public Stream open(int flags) throws PosixException {
        return open(flags, 0);
    }

    public Stream open(int flags, @SuppressWarnings("unused") int mode) throws PosixException {
        int rdwr = flags & 0x3;
        switch (rdwr) {
            case O_RDONLY:
                if (BitTest.test(flags, O_TMPFILE)) {
                    throw new PosixException(Errno.EINVAL);
                }
                return open(true, false);
            case O_WRONLY:
                if (BitTest.test(flags, O_TMPFILE)) {
                    throw new PosixException(Errno.EINVAL);
                }
                return open(false, true);
            case O_RDWR:
                if (BitTest.test(flags, O_TMPFILE)) {
                    throw new PosixException(Errno.EINVAL);
                }
                return open(true, true);
            default:
                throw new PosixException(Errno.EINVAL);
        }
    }

    @Override
    public void stat(Stat buf) throws PosixException {
        super.stat(buf);
        buf.st_nlink = 1;
        buf.st_mode |= Stat.S_IFREG;
    }

    @Override
    public void statx(int mask, Statx buf) throws PosixException {
        super.statx(mask, buf);
        if (BitTest.test(mask, Stat.STATX_NLINK)) {
            buf.stx_nlink = 1;
            buf.stx_mask |= Stat.STATX_NLINK;
        }
        if (BitTest.test(mask, Stat.STATX_TYPE)) {
            buf.stx_mode |= Stat.S_IFREG;
            buf.stx_mask |= Stat.STATX_TYPE;
        }
    }

    @Override
    public String toString() {
        return "VFSFile[" + getPath() + "]";
    }
}
