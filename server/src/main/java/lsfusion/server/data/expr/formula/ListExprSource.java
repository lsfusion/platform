package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;

public abstract class ListExprSource extends ContextListExprType implements ExprSource {

    public ListExprSource(ImList<? extends Expr> exprs) {
        super(exprs);
    }

    public abstract CompileSource getCompileSource();

    public String getSource(int i) {
        return exprs.get(i).getSource(getCompileSource());
    }

    public SQLSyntax getSyntax() {
        return getCompileSource().syntax;
    }

    public ExecuteEnvironment getEnv() {
        return getCompileSource().env;
    }

    public KeyType getKeyType() {
        return getCompileSource().keyType;
    }
}
