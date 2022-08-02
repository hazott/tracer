package org.graalvm.vm.trcview.data.ir;

import org.graalvm.vm.util.HexFormatter;

public class IndexedMemoryOperand extends Operand {
    private final int register;
    private final long offset;

    public IndexedMemoryOperand(int register) {
        this.register = register;
        this.offset = 0;
    }

    public IndexedMemoryOperand(int register, long offset) {
        this.register = register;
        this.offset = offset;
    }

    public int getRegister() {
        return register;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public int hashCode() {
        return (int) (offset ^ (offset >> 32)) ^ register;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof IndexedMemoryOperand)) {
            return false;
        }
        IndexedMemoryOperand m = (IndexedMemoryOperand) o;
        return m.offset == offset && m.register == register;
    }

    @Override
    public String toString() {
        if (offset == 0) {
            return "(R" + register + ")";
        } else if (offset < 0) {
            return "-0x" + HexFormatter.tohex(-offset) + "(R" + register + ")";
        } else {
            return "0x" + HexFormatter.tohex(offset) + "(R" + register + ")";
        }
    }
}
