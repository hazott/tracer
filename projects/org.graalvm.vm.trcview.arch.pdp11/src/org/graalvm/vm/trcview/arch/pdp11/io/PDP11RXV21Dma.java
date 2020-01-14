package org.graalvm.vm.trcview.arch.pdp11.io;

import java.io.IOException;

import org.graalvm.vm.trcview.arch.io.DeviceEvent;
import org.graalvm.vm.trcview.arch.pdp11.PDP11;
import org.graalvm.vm.util.io.WordInputStream;
import org.graalvm.vm.util.io.WordOutputStream;

public class PDP11RXV21Dma extends DeviceEvent {
    private final short type;
    private final short rx2wc;
    private final short rx2ba;

    public PDP11RXV21Dma(WordInputStream in, int tid) throws IOException {
        super(PDP11.ID, tid);
        type = in.read16bit();
        rx2wc = in.read16bit();
        rx2ba = in.read16bit();
        in.read16bit();
    }

    @Override
    protected void writeRecord(WordOutputStream out) throws IOException {
        out.write16bit(type);
        out.write16bit(rx2wc);
        out.write16bit(rx2ba);
    }
}