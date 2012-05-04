package platform.server.data.expr.where.extra;

import platform.interop.Compare;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.where.Where;

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
    public String getSource(CompileSource compile) {
        return operator1.getSource(compile) + " = ANY(" + operator2.getSource(compile) + ")";
    }

    @Override
    protected int hash(HashContext hash) {
        return operator1.hashOuter(hash) * 31 + operator2.hashOuter(hash) + 15;
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        return create(operator1, operator2, new InArrayWhere(operator1, operator2));
    }
}
