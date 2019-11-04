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
package org.graalvm.vm.x86;

import org.graalvm.vm.memory.VirtualMemory;
import org.graalvm.vm.x86.node.MemoryReadNode;
import org.graalvm.vm.x86.node.MemoryWriteNode;
import org.graalvm.vm.x86.node.flow.TraceRegistry;
import org.graalvm.vm.x86.substitution.SubstitutionRegistry;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.frame.FrameSlot;

public class ArchitecturalState {
    private final RegisterAccessFactory registerAccess;
    private final VirtualMemory memory;
    private final FrameSlot instructionCount;
    private final FrameSlot cpuState;
    private final FrameSlot trace;
    private final FrameSlot isTrace;
    private final TraceRegistry traces;
    private final SubstitutionRegistry substitutions;
    private final Assumption singleThreaded;

    public ArchitecturalState(AMD64Context context) {
        registerAccess = new RegisterAccessFactory(context.getGPRs(), context.getZMMs(), context.getXMMs(), context.getXMMF32(), context.getXMMF64(), context.getXMMType(), context.getFS(),
                        context.getGS(), context.getPC(), context.getCF(), context.getPF(), context.getAF(), context.getZF(), context.getSF(), context.getDF(), context.getOF(), context.getAC(),
                        context.getID());
        memory = context.getMemory();
        instructionCount = context.getInstructionCount();
        cpuState = context.getDispatchCpuState();
        trace = context.getDispatchTrace();
        traces = context.getTraceRegistry();
        substitutions = context.getSubstitutionRegistry();
        singleThreaded = context.getSingleThreadedAssumption();
        isTrace = context.getTrace();
    }

    public RegisterAccessFactory getRegisters() {
        return registerAccess;
    }

    public VirtualMemory getMemory() {
        return memory;
    }

    public MemoryReadNode createMemoryRead() {
        return new MemoryReadNode(memory);
    }

    public MemoryWriteNode createMemoryWrite() {
        return new MemoryWriteNode(memory);
    }

    public FrameSlot getInstructionCount() {
        return instructionCount;
    }

    public FrameSlot getDispatchCpuState() {
        return cpuState;
    }

    public FrameSlot getDispatchTrace() {
        return trace;
    }

    public FrameSlot getTrace() {
        return isTrace;
    }

    public TraceRegistry getTraceRegistry() {
        return traces;
    }

    public SubstitutionRegistry getSubstitutions() {
        return substitutions;
    }

    public Assumption getSingleThreadedAssumption() {
        return singleThreaded;
    }
}
