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
package org.graalvm.vm.x86.trcview.io;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.graalvm.vm.util.log.Levels;
import org.graalvm.vm.util.log.Trace;
import org.graalvm.vm.x86.isa.AMD64InstructionQuickInfo;
import org.graalvm.vm.x86.node.debug.trace.CallArgsRecord;
import org.graalvm.vm.x86.node.debug.trace.ExecutionTraceReader;
import org.graalvm.vm.x86.node.debug.trace.Record;
import org.graalvm.vm.x86.node.debug.trace.StepRecord;
import org.graalvm.vm.x86.trcview.analysis.Analysis;

public class BlockNode extends Node {
    private static Logger log = Trace.create(BlockNode.class);

    private StepRecord head;
    private CallArgsRecord callArgs;
    private List<Node> children;

    public BlockNode(StepRecord head, List<Node> children) {
        this(head, children, null);
    }

    public BlockNode(StepRecord head, List<Node> children, CallArgsRecord args) {
        this.head = head;
        this.callArgs = args;
        this.children = children;
        for (Node n : children) {
            n.setParent(this);
        }
    }

    public StepRecord getHead() {
        return head;
    }

    public CallArgsRecord getCallArguments() {
        return callArgs;
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(children);
    }

    public Node getFirstNode() {
        for (Node n : children) {
            if (n instanceof BlockNode || n instanceof RecordNode && ((RecordNode) n).getRecord() instanceof StepRecord) {
                return n;
            }
        }
        return null;
    }

    public StepRecord getFirstStep() {
        for (Node n : children) {
            if (n instanceof RecordNode && ((RecordNode) n).getRecord() instanceof StepRecord) {
                return (StepRecord) ((RecordNode) n).getRecord();
            } else if (n instanceof BlockNode) {
                return ((BlockNode) n).getFirstStep();
            }
        }
        return null;
    }

    public static BlockNode read(ExecutionTraceReader in, Analysis analysis) throws IOException {
        return read(in, analysis, null);
    }

    public static BlockNode read(ExecutionTraceReader in, Analysis analysis, ProgressListener progress) throws IOException {
        List<Node> nodes = new ArrayList<>();
        Node node;
        long tid = 0;
        while ((node = parseRecord(in, analysis, progress, tid)) != null) {
            nodes.add(node);
            if (tid == 0) {
                if (node instanceof RecordNode) {
                    tid = ((RecordNode) node).getRecord().getTid();
                } else if (node instanceof BlockNode) {
                    tid = ((BlockNode) node).getFirstStep().getTid();
                }
            }
        }
        return new BlockNode(null, nodes);
    }

    private static Node parseRecord(ExecutionTraceReader in, Analysis analysis, ProgressListener progress, long thread) throws IOException {
        long tid = thread;
        Record record = null;
        try {
            record = in.read();
        } catch (EOFException e) {
            log.log(Levels.WARNING, "Unexpected EOF", e);
        }
        if (record == null) {
            return null;
        }
        while (tid != 0 && record.getTid() != tid) {
            record = in.read();
            if (record == null) {
                return null;
            }
        }
        if (tid == 0) {
            tid = record.getTid();
        }
        analysis.process(record);
        if (record instanceof StepRecord) {
            StepRecord step = (StepRecord) record;
            if (AMD64InstructionQuickInfo.isCall(step.getMachinecode())) {
                if (progress != null) {
                    progress.progressUpdate(in.tell());
                }
                List<Node> result = new ArrayList<>();
                CallArgsRecord args = null;
                int cnt = 0;
                while (true) {
                    Node child = parseRecord(in, analysis, progress, tid);
                    if (child == null) {
                        break;
                    }
                    result.add(child);
                    if (progress != null && cnt > 10_000) {
                        cnt = 0;
                        progress.progressUpdate(in.tell());
                    } else {
                        cnt++;
                    }
                    if (args == null && child instanceof RecordNode && ((RecordNode) child).getRecord() instanceof CallArgsRecord) {
                        args = (CallArgsRecord) ((RecordNode) child).getRecord();
                    }
                    if (child instanceof RecordNode && ((RecordNode) child).getRecord() instanceof StepRecord) {
                        StepRecord s = (StepRecord) ((RecordNode) child).getRecord();
                        if (AMD64InstructionQuickInfo.isRet(s.getMachinecode())) {
                            if (progress != null) {
                                progress.progressUpdate(in.tell());
                            }
                            break;
                        }
                    }
                }
                return new BlockNode(step, result, args);
            } else {
                return new RecordNode(record);
            }
        } else {
            return new RecordNode(record);
        }
    }
}
