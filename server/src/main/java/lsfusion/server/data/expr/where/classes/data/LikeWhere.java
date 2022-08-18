package lsfusion.server.data.expr.where.classes.data;

import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.data.StringClass;

public class LikeWhere extends BinaryWhere<LikeWhere> {

    private LikeWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    protected LikeWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new LikeWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.CONTAINS;
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return (operator1.hashOuter(hashContext) * 31 + operator2.hashOuter(hashContext)) * 31;
    }

    protected String getCompareSource(CompileSource compile) {
        throw new RuntimeException("not supported");
    }

    @Override
    protected String getBaseSource(CompileSource compile) {
        Type type = operator1.getType(compile.keyType);
        String likeString = type instanceof StringClass && ((StringClass) type).caseInsensitive ? " " + compile.syntax.getInsensitiveLike() + " " : " LIKE ";
        return operator1.getSource(compile) + likeString + "(" + operator2.getSource(compile) + ")";
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(checkEquals(operator1, operator2))
            return operator1.getWhere();
        return create(operator1, operator2, new LikeWhere(operator1, operator2));
    }
}
