package org.graalvm.vm.x86.trcview.expression.ast;

import org.graalvm.vm.x86.trcview.expression.EvaluationException;
import org.graalvm.vm.x86.trcview.expression.ExpressionContext;

public class OrNode extends Expression {
    public final Expression left;
    public final Expression right;

    public OrNode(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public long evaluate(ExpressionContext ctx) throws EvaluationException {
        return left.evaluate(ctx) | right.evaluate(ctx);
    }

    @Override
    public String toString() {
        return "(" + left + " | " + right + ")";
    }
}