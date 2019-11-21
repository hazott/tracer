package org.graalvm.vm.x86.trcview.io.data.pdp11;

import java.io.IOException;

import org.graalvm.vm.util.io.WordInputStream;
import org.graalvm.vm.util.io.WordOutputStream;
import org.graalvm.vm.x86.trcview.arch.PDP11;
import org.graalvm.vm.x86.trcview.io.data.DeviceEvent;

public class PDP11TrapEvent extends DeviceEvent {
    private final short trap;
    private final short cause;

    public PDP11TrapEvent(WordInputStream in, int tid) throws IOException {
        super(PDP11.ID, tid);
        trap = in.read16bit();
        cause = in.read16bit();
    }

    @Override
    protected void writeRecord(WordOutputStream out) throws IOException {
        out.write16bit(trap);
        out.write16bit(cause);
    }
}
