package org.graalvm.vm.trcview.ui.data;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.graalvm.vm.trcview.arch.io.StepEvent;
import org.graalvm.vm.trcview.arch.io.StepFormat;
import org.graalvm.vm.trcview.data.ChainTarget;
import org.graalvm.vm.trcview.data.MemoryChainTarget;
import org.graalvm.vm.trcview.data.RegisterChainTarget;
import org.graalvm.vm.trcview.data.RegisterTypeMap;
import org.graalvm.vm.trcview.data.Semantics;
import org.graalvm.vm.trcview.data.ir.RegisterOperand;
import org.graalvm.vm.trcview.data.type.VariableType;
import org.graalvm.vm.trcview.net.TraceAnalyzer;
import org.graalvm.vm.trcview.ui.Utils;
import org.graalvm.vm.trcview.ui.event.StepListenable;
import org.graalvm.vm.trcview.ui.event.StepListener;
import org.graalvm.vm.util.BitTest;

@SuppressWarnings("serial")
public class TypeRecoveryDialog extends JDialog implements StepListener {
    private TraceAnalyzer trc;

    private JTextPane text;

    public TypeRecoveryDialog(JFrame owner, TraceAnalyzer trc, StepListenable step) {
        super(owner, "Type Recovery", false);

        setLayout(new BorderLayout());

        JLabel status = new JLabel("Ready");

        text = new JTextPane();
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        text.setEditable(false);
        text.setContentType("text/plain");
        add(BorderLayout.CENTER, new JScrollPane(text));
        add(BorderLayout.SOUTH, status);

        if (trc != null) {
            setTraceAnalyzer(trc);
        }

        step.addStepListener(this);

        setSize(800, 600);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                step.removeStepListener(TypeRecoveryDialog.this);
                dispose();
            }
        });
    }

    public void setTraceAnalyzer(TraceAnalyzer trc) {
        this.trc = trc;
        StepEvent step = Utils.getStep(trc.getInstruction(0));
        if (step != null) {
            setStep(step);
        }
    }

    public void setStep(StepEvent step) {
        int regcount = trc.getArchitecture().getRegisterCount();
        long pc = step.getPC();
        StringBuilder buf = new StringBuilder();
        if (trc.getTypeRecovery() == null) {
            return;
        }

        Semantics semantics = trc.getTypeRecovery().getSemantics();
        for (int i = 0; i < regcount; i++) {
            long flags = semantics.resolve(pc, new RegisterOperand(i));
            buf.append("register ");
            buf.append(String.format("%02d", i));
            buf.append(":");
            if (flags == 0) {
                buf.append(" [ ] --\n");
            } else if (flags == VariableType.CHAIN_BIT) {
                buf.append(" [C] --\n");
            } else {
                if (BitTest.test(flags, VariableType.CHAIN_BIT)) {
                    buf.append(" [C]");
                } else {
                    buf.append(" [ ]");
                }
                for (VariableType type : VariableType.getTypeConstraints()) {
                    if (BitTest.test(flags, type.getMask())) {
                        buf.append(' ');
                        buf.append(type.getName());
                    }
                }
                buf.append(" => ");
                buf.append(VariableType.resolve(flags, trc.getArchitecture().getTypeInfo().getPointerSize()));
                buf.append('\n');
            }
        }

        // debugging feature: show data per position
        buf.append("\n\nDEBUG:\n");
        for (int i = 0; i < regcount; i++) {
            long flags = semantics.get(pc, new RegisterOperand(i));
            buf.append("register ");
            buf.append(String.format("%02d", i));
            buf.append(":");
            if (flags == 0) {
                buf.append(" [ ] --\n");
            } else if (flags == VariableType.CHAIN_BIT) {
                buf.append(" [C] --\n");
            } else {
                if (BitTest.test(flags, VariableType.CHAIN_BIT)) {
                    buf.append(" [C]");
                } else {
                    buf.append(" [ ]");
                }
                for (VariableType type : VariableType.getTypeConstraints()) {
                    if (BitTest.test(flags, type.getMask())) {
                        buf.append(' ');
                        buf.append(type.getName());
                    }
                }
                buf.append('\n');
            }
        }

        StepFormat fmt = trc.getArchitecture().getFormat();
        buf.append("\nLinks:\n");
        buf.append("Implicit: ").append(fmt.formatAddress(semantics.getChain(pc))).append('\n');
        buf.append("Explicit:");
        for (RegisterTypeMap map : semantics.getExtraChain(pc)) {
            buf.append(' ');
            buf.append(fmt.formatAddress(map.getPC()));
        }
        buf.append("\nForward:");
        for (RegisterTypeMap map : semantics.getForwardChain(pc)) {
            buf.append(' ');
            buf.append(fmt.formatAddress(map.getPC()));
        }
        buf.append("\nClosure per register:\n");

        for (int i = 0; i < regcount; i++) {
            Set<ChainTarget> set = new HashSet<>();
            semantics.resolve(pc, new RegisterOperand(i), set);
            buf.append("register ");
            buf.append(String.format("%02d", i));
            buf.append(":");
            set.stream().map(x -> {
                if (x instanceof RegisterChainTarget) {
                    RegisterChainTarget tgt = (RegisterChainTarget) x;
                    return fmt.formatAddress(tgt.map.getPC()) + "[r" + tgt.register + "]";
                } else if (x instanceof MemoryChainTarget) {
                    MemoryChainTarget tgt = (MemoryChainTarget) x;
                    return "[" + fmt.formatAddress(tgt.address) + "]";
                } else {
                    return "?";
                }
            }).sorted().forEach(x -> buf.append(' ').append(x));
            buf.append('\n');
        }

        text.setText(buf.toString());
        text.setCaretPosition(0);
    }
}
