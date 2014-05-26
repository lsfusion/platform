package lsfusion.server.data.expr.where.extra;

import lsfusion.interop.Compare;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.where.Where;

public class InArrayWhere extends BinaryWhere<InArrayWhere> {

    public InArrayWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    protected InArrayWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new InArrayWhere(operator1, operator2);
    }

    @Override
    protected Compare getCompare() {
        return Compare.INARRAY;
    }

    protected String getCompareSource(CompileSource compile) {
        throw new RuntimeException("not supported");
    }

    @Override
    protected String getBaseSource(CompileSource compile) {
        return compile.syntax.getInArray(operator1.getSource(compile), operator2.getSource(compile));
    }

    @Override
    protected int hash(HashContext hash) {
        return operator1.hashOuter(hash) * 31 + operator2.hashOuter(hash) + 15;
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        return create(operator1, operator2, new InArrayWhere(operator1, operator2));
    }
}
