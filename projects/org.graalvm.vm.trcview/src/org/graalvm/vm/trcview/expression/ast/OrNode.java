package org.graalvm.vm.trcview.expression.ast;

import java.util.Map;
import java.util.Objects;

import org.graalvm.vm.trcview.expression.EvaluationException;
import org.graalvm.vm.trcview.expression.ExpressionContext;

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
    public Expression materialize(Map<String, Long> vars) {
        Expression l = left.materialize(vars);
        Expression r = right.materialize(vars);
        if (l != left || r != right) {
            return new OrNode(l, r);
        } else {
            return this;
        }
    }

    @Override
    protected String str(boolean par) {
        String l;
        String r;

        if (left instanceof OrNode) {
            l = left.str(false);
        } else {
            l = left.str(true);
        }

        if (right instanceof OrNode) {
            r = right.str(false);
        } else {
            r = right.str(true);
        }

        if (par) {
            return "(" + l + " | " + r + ")";
        } else {
            return l + " | " + r;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof OrNode)) {
            return false;
        }
        OrNode n = (OrNode) o;
        return left.equals(n.left) && right.equals(n.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
