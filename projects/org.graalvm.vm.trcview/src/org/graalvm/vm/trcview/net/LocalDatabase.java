package org.graalvm.vm.trcview.net;

import java.awt.Color;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.graalvm.vm.posix.elf.Symbol;
import org.graalvm.vm.posix.elf.SymbolResolver;
import org.graalvm.vm.trcview.analysis.Analysis;
import org.graalvm.vm.trcview.analysis.ComputedSymbol;
import org.graalvm.vm.trcview.analysis.MappedFiles;
import org.graalvm.vm.trcview.analysis.Search;
import org.graalvm.vm.trcview.analysis.SymbolRenameListener;
import org.graalvm.vm.trcview.analysis.SymbolTable;
import org.graalvm.vm.trcview.analysis.memory.MemoryNotMappedException;
import org.graalvm.vm.trcview.analysis.memory.MemoryRead;
import org.graalvm.vm.trcview.analysis.memory.MemoryTrace;
import org.graalvm.vm.trcview.analysis.memory.MemoryUpdate;
import org.graalvm.vm.trcview.analysis.type.Prototype;
import org.graalvm.vm.trcview.arch.Architecture;
import org.graalvm.vm.trcview.arch.io.CpuState;
import org.graalvm.vm.trcview.arch.io.IoEvent;
import org.graalvm.vm.trcview.expression.EvaluationException;
import org.graalvm.vm.trcview.info.Expressions;
import org.graalvm.vm.trcview.info.FormattedExpression;
import org.graalvm.vm.trcview.io.BlockNode;
import org.graalvm.vm.trcview.io.Node;
import org.graalvm.vm.trcview.storage.SessionStorage;
import org.graalvm.vm.trcview.storage.sql.SQLSessionStorage;
import org.graalvm.vm.trcview.ui.event.ChangeListener;
import org.graalvm.vm.util.log.Levels;
import org.graalvm.vm.util.log.Trace;

public class LocalDatabase implements TraceAnalyzer {
    private static final Logger log = Trace.create(TraceAnalyzer.class);

    private SessionStorage session;
    private Architecture arch;
    private SymbolResolver resolver;
    private SymbolTable symbols;
    private BlockNode root;
    private MemoryTrace memory;
    private MappedFiles files;
    private List<Node> syscalls;
    private Map<Integer, List<IoEvent>> io;
    private long steps;
    private List<ChangeListener> symbolChangeListeners;
    private List<ChangeListener> commentChangeListeners;
    private List<SymbolRenameListener> symbolRenameListeners;
    private Expressions expressions;

    public LocalDatabase(Architecture arch, BlockNode root, Analysis analysis) {
        this.arch = arch;
        this.root = root;

        try {
            session = new SQLSessionStorage();
            session.setTrace(UUID.randomUUID().toString());
        } catch (SQLException e) {
            log.log(Levels.FATAL, "Cannot open session storage: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        resolver = analysis.getSymbolResolver();
        symbols = analysis.getComputedSymbolTable();
        for (ComputedSymbol sym : symbols.getSymbols()) {
            session.createComputedSymbol(sym);
        }
        memory = analysis.getMemoryTrace();
        files = analysis.getMappedFiles();
        syscalls = analysis.getSyscalls();
        io = analysis.getIo();
        steps = analysis.getStepCount();
        symbolRenameListeners = new ArrayList<>();
        symbolChangeListeners = new ArrayList<>();
        commentChangeListeners = new ArrayList<>();
        expressions = new Expressions();
    }

    @Override
    public Symbol getSymbol(long pc) {
        return resolver.getSymbol(pc);
    }

    @Override
    public ComputedSymbol getComputedSymbol(long pc) {
        return session.getComputedSymbol(pc);
    }

    @Override
    public void renameSymbol(ComputedSymbol sym, String name) {
        session.renameSymbol(sym, name);
        sym.name = name;
        fireSymbolRenameEvent(sym);
    }

    @Override
    public void setPrototype(ComputedSymbol sym, Prototype prototype) {
        session.setPrototype(sym, prototype);
    }

    @Override
    public Set<ComputedSymbol> getSubroutines() {
        return session.getSubroutines();
    }

    @Override
    public Set<ComputedSymbol> getLocations() {
        return session.getLocations();
    }

    @Override
    public Collection<ComputedSymbol> getSymbols() {
        return session.getSymbols();
    }

    @Override
    public Map<String, List<ComputedSymbol>> getNamedSymbols() {
        return session.getNamedSymbols();
    }

    @Override
    public void addSymbolRenameListener(SymbolRenameListener listener) {
        symbolRenameListeners.add(listener);
    }

    protected void fireSymbolRenameEvent(ComputedSymbol sym) {
        for (SymbolRenameListener l : symbolRenameListeners) {
            try {
                l.symbolRenamed(sym);
            } catch (Throwable t) {
                log.log(Levels.WARNING, "SymbolRenameListener failed: " + t, t);
            }
        }
    }

    @Override
    public void addSymbolChangeListener(ChangeListener listener) {
        symbolChangeListeners.add(listener);
    }

    @Override
    public void addSubroutine(long pc, String name, Prototype prototype) {
        session.addSubroutine(pc, name, prototype);
    }

    private void analyzeBlock(BlockNode block) {
        for (Node node : block.getNodes()) {
            symbols.visit(node);
            if (node instanceof BlockNode) {
                analyzeBlock((BlockNode) node);
            }
        }
    }

    @Override
    public void reanalyze() {
        getSymbols().forEach(ComputedSymbol::resetVisits);
        analyzeBlock(root);
        symbols.cleanup();
        for (ChangeListener l : symbolChangeListeners) {
            try {
                l.valueChanged();
            } catch (Throwable t) {
                log.warning("Error while executing listener: " + l);
            }
        }
    }

    @Override
    public void refresh() {
        // nothing
    }

    @Override
    public long getInstructionCount() {
        return steps;
    }

    @Override
    public BlockNode getRoot() {
        return root;
    }

    @Override
    public BlockNode getParent(Node node) {
        return node.getParent();
    }

    @Override
    public BlockNode getChildren(BlockNode node) {
        return node;
    }

    @Override
    public Node getNode(Node node) {
        return node;
    }

    @Override
    public List<Node> getSyscalls() {
        return syscalls;
    }

    @Override
    public Map<Integer, List<IoEvent>> getIo() {
        return io;
    }

    @Override
    public Node getInstruction(long insn) {
        return Search.instruction(root, insn);
    }

    @Override
    public Node getNextStep(Node node) {
        return Search.nextStep(node);
    }

    @Override
    public Node getPreviousStep(Node node) {
        return Search.previousStep(node);
    }

    @Override
    public Node getNextPC(Node node, long pc) {
        return Search.nextPC(node, pc);
    }

    @Override
    public byte getI8(long address, long insn) throws MemoryNotMappedException {
        return memory.getByte(address, insn);
    }

    @Override
    public long getI64(long address, long insn) throws MemoryNotMappedException {
        return memory.getWord(address, insn);
    }

    @Override
    public MemoryRead getLastRead(long address, long insn) throws MemoryNotMappedException {
        return memory.getLastRead(address, insn);
    }

    @Override
    public MemoryRead getNextRead(long address, long insn) throws MemoryNotMappedException {
        return memory.getNextRead(address, insn);
    }

    @Override
    public MemoryUpdate getLastWrite(long address, long insn) throws MemoryNotMappedException {
        return memory.getLastWrite(address, insn);
    }

    @Override
    public MemoryUpdate getNextWrite(long address, long insn) throws MemoryNotMappedException {
        return memory.getNextWrite(address, insn);
    }

    @Override
    public List<MemoryUpdate> getPreviousWrites(long address, long insn, long count) throws MemoryNotMappedException {
        return memory.getPreviousWrites(address, insn, count);
    }

    @Override
    public Node getMapNode(long address, long insn) throws MemoryNotMappedException {
        return memory.getMapNode(address, insn);
    }

    @Override
    public long getBase(long pc) {
        return files.getBase(pc);
    }

    @Override
    public long getLoadBias(long pc) {
        return files.getLoadBias(pc);
    }

    @Override
    public long getOffset(long pc) {
        return files.getOffset(pc);
    }

    @Override
    public long getFileOffset(long pc) {
        return files.getFileOffset(pc);
    }

    @Override
    public String getFilename(long pc) {
        return files.getFilename(pc);
    }

    @Override
    public Architecture getArchitecture() {
        return arch;
    }

    @Override
    public void addCommentChangeListener(ChangeListener l) {
        commentChangeListeners.add(l);
    }

    protected void fireCommentChanged() {
        for (ChangeListener l : commentChangeListeners) {
            try {
                l.valueChanged();
            } catch (Throwable t) {
                log.warning("Error while executing listener: " + l);
            }
        }
    }

    @Override
    public void setCommentForPC(long pc, String comment) {
        session.setCommentForPC(pc, comment);
        fireCommentChanged();
    }

    @Override
    public String getCommentForPC(long pc) {
        return session.getCommentForPC(pc);
    }

    @Override
    public void setCommentForInsn(long insn, String comment) {
        session.setCommentForInsn(insn, comment);
        fireCommentChanged();
    }

    @Override
    public String getCommentForInsn(long insn) {
        return session.getCommentForInsn(insn);
    }

    @Override
    public Map<Long, String> getCommentsForInsns() {
        return session.getCommentsForInsns();
    }

    @Override
    public Map<Long, String> getCommentsForPCs() {
        return session.getCommentsForPCs();
    }

    @Override
    public void setExpression(long pc, String expression) throws ParseException {
        expressions.setExpression(pc, arch.getFormat(), expression);
        fireCommentChanged();
    }

    @Override
    public String getExpression(long pc) {
        FormattedExpression expr = expressions.getExpression(pc);
        if (expr == null) {
            return null;
        } else {
            return expr.getExpression();
        }
    }

    @Override
    public String evaluateExpression(CpuState state) throws EvaluationException {
        return expressions.evaluate(state, this);
    }

    @Override
    public Map<Long, String> getExpressions() {
        return expressions.getExpressions();
    }

    @Override
    public void setColor(long pc, Color color) {
        session.setColor(pc, color);
        fireCommentChanged();
    }

    @Override
    public Color getColor(CpuState state) {
        return session.getColor(state.getPC());
    }

    @Override
    public Map<Long, Color> getColors() {
        return session.getColors();
    }
}