package lsfusion.server.data.expr.where.extra;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.interop.Compare;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public class LikeWhere extends BinaryWhere<LikeWhere> {

    private final Boolean isStartWith;

    private LikeWhere(BaseExpr operator1, BaseExpr operator2, Boolean startWithType) {
        super(operator1, operator2);
        this.isStartWith = startWithType;
    }

    protected LikeWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new LikeWhere(operator1, operator2, isStartWith);
    }

    protected Compare getCompare() {
        return isStartWith == null ?
                Compare.LIKE :
                isStartWith ?
                        Compare.START_WITH :
                        Compare.CONTAINS;
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return (operator1.hashOuter(hashContext) * 31 + operator2.hashOuter(hashContext)) * 31 + (isStartWith == null ? 0 : (isStartWith ? 1 : 2));
    }

    @Override
    public boolean calcTwins(TwinImmutableObject obj) {
        return super.calcTwins(obj) && BaseUtils.nullEquals(isStartWith, ((LikeWhere)obj).isStartWith);
    }

    protected String getCompareSource(CompileSource compile) {
        throw new RuntimeException("not supported");
    }

    @Override
    protected String getBaseSource(CompileSource compile) {
        Type type = operator1.getType(compile.keyType);
        String likeString = type instanceof StringClass && ((StringClass) type).caseInsensitive ? " " + compile.syntax.getInsensitiveLike() + " " : " LIKE ";

        return operator1.getSource(compile)
               + likeString
               + "(" + (isStartWith != null && !isStartWith ? "'%' || " : "") + operator2.getSource(compile) + (isStartWith != null ? " || '%'" : "") + ")";
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2, Boolean isStartWith) {
        return create(operator1, operator2, new LikeWhere(operator1, operator2, isStartWith));
    }
}
