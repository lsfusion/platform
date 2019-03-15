package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;

public abstract class ListExprSource extends ContextListExprType implements ExprSource {

    private final boolean needValue;
    public ListExprSource(ImList<? extends Expr> exprs, boolean needValue) {
        super(exprs);
        this.needValue = needValue;
    }

    public abstract CompileSource getCompileSource();

    public String getSource(int i) {
        return exprs.get(i).getSource(getCompileSource(), needValue);
    }

    public SQLSyntax getSyntax() {
        return getCompileSource().syntax;
    }

    public MStaticExecuteEnvironment getMEnv() {
        return getCompileSource().env;
    }

    public KeyType getKeyType() {
        return getCompileSource().keyType;
    }

    public boolean isToString() {
        return false;
    }
}
