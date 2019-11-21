package org.graalvm.vm.x86.trcview.net.protocol.cmdresult;

import java.io.IOException;

import org.graalvm.vm.util.io.WordInputStream;
import org.graalvm.vm.util.io.WordOutputStream;
import org.graalvm.vm.x86.trcview.net.protocol.cmd.Command;

public class GetBaseResult extends Result {
    private long base;

    public GetBaseResult() {
        super(Command.GET_BASE);
    }

    public GetBaseResult(long base) {
        super(Command.GET_BASE);
        this.base = base;
    }

    public long getBase() {
        return base;
    }

    @Override
    public void read(WordInputStream in) throws IOException {
        base = in.read64bit();
    }

    @Override
    public void write(WordOutputStream out) throws IOException {
        out.write64bit(base);
    }
}