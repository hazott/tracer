package org.graalvm.vm.trcview.net.protocol.cmd;

import java.io.IOException;

import org.graalvm.vm.util.io.WordInputStream;
import org.graalvm.vm.util.io.WordOutputStream;

public class GetLastWrite extends Command {
    private long address;
    private long insn;

    public GetLastWrite() {
        super(Command.GET_LAST_WRITE);
    }

    public GetLastWrite(long address, long insn) {
        super(Command.GET_LAST_WRITE);
        this.address = address;
        this.insn = insn;
    }

    public long getAddress() {
        return address;
    }

    public long getInstructionCount() {
        return insn;
    }

    @Override
    public void read(WordInputStream in) throws IOException {
        address = in.read64bit();
        insn = in.read64bit();
    }

    @Override
    public void write(WordOutputStream out) throws IOException {
        out.write64bit(address);
        out.write64bit(insn);
    }
}