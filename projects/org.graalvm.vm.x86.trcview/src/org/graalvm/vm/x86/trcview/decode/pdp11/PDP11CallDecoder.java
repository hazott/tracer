package org.graalvm.vm.x86.trcview.decode.pdp11;

import org.graalvm.vm.x86.trcview.analysis.type.DataType;
import org.graalvm.vm.x86.trcview.analysis.type.Function;
import org.graalvm.vm.x86.trcview.analysis.type.Prototype;
import org.graalvm.vm.x86.trcview.analysis.type.Type;
import org.graalvm.vm.x86.trcview.decode.CallDecoder;
import org.graalvm.vm.x86.trcview.expression.EvaluationException;
import org.graalvm.vm.x86.trcview.expression.ExpressionContext;
import org.graalvm.vm.x86.trcview.io.data.CpuState;
import org.graalvm.vm.x86.trcview.io.data.pdp11.PDP11CpuState;
import org.graalvm.vm.x86.trcview.net.TraceAnalyzer;

public class PDP11CallDecoder extends CallDecoder {
    public static String decode(Function function, PDP11CpuState state, PDP11CpuState nextState, TraceAnalyzer trc) {
        StringBuilder buf = new StringBuilder(function.getName());
        buf.append('(');
        Prototype prototype = function.getPrototype();
        for (int i = 0; i < prototype.args.size(); i++) {
            Type type = prototype.args.get(i);
            long val;
            if (type.getExpression() != null) {
                try {
                    ExpressionContext ctx = new ExpressionContext(state, trc);
                    val = type.getExpression().evaluate(ctx);
                } catch (EvaluationException e) {
                    val = 0;
                }
            } else {
                return null;
            }
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(str(type, val, state, trc));
        }
        buf.append(')');
        if (nextState != null) {
            long retval;
            if (prototype.expr != null) {
                try {
                    ExpressionContext ctx = new ExpressionContext(nextState, trc);
                    retval = prototype.expr.evaluate(ctx);
                } catch (EvaluationException e) {
                    retval = 0;
                }
            } else if (prototype.returnType.getType() == DataType.VOID) {
                retval = 0;
            } else {
                return null;
            }
            String s = str(prototype.returnType, retval, nextState, trc);
            if (s.length() > 0) {
                buf.append(" = ");
                buf.append(s);
            }
        }
        return buf.toString();
    }

    @Override
    public String decode(Function function, CpuState state, CpuState nextState, TraceAnalyzer trc) {
        if (!(state instanceof PDP11CpuState) || !(nextState instanceof PDP11CpuState)) {
            return null;
        }
        return decode(function, (PDP11CpuState) state, (PDP11CpuState) nextState, trc);
    }
}
