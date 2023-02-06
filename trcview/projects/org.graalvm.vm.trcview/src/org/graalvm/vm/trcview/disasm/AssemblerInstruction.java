package org.graalvm.vm.trcview.disasm;

public class AssemblerInstruction {
    private String mnemonic;
    private Operand[] operands;

    public AssemblerInstruction(String mnemonic) {
        this.mnemonic = mnemonic;
        operands = new Operand[0];
    }

    public AssemblerInstruction(String mnemonic, Operand[] operands) {
        this.mnemonic = mnemonic;
        this.operands = operands;
    }

    public String getMnemonic() {
        return mnemonic;
    }

    public Operand[] getOperands() {
        return operands;
    }

    public String[] getComponents() {
        String[] result = new String[operands.length + 1];
        for (int i = 0; i < operands.length; i++) {
            result[i + 1] = operands[i].toString();
        }
        result[0] = mnemonic;
        return result;
    }

    @Override
    public String toString() {
        return mnemonic + " " + String.join(", ", getComponents());
    }
}
