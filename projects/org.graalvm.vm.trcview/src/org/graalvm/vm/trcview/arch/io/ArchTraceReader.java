package org.graalvm.vm.trcview.arch.io;

import java.io.IOException;

import org.graalvm.vm.trcview.analysis.Analyzer;

public abstract class ArchTraceReader {
    public Analyzer getAnalyzer() {
        return null;
    }

    public abstract Event read() throws IOException;

    public abstract long tell();
}
