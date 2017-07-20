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

    private final Integer compareType; //1 - startsWith, 2 - contains, 3 - endsWith

    private LikeWhere(BaseExpr operator1, BaseExpr operator2, Integer compareType) {
        super(operator1, operator2);
        this.compareType = compareType;
    }

    protected LikeWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new LikeWhere(operator1, operator2, compareType);
    }

    protected Compare getCompare() {
        return compareType == null ? Compare.LIKE : compareType.equals(1) ? Compare.START_WITH : compareType.equals(2) ? Compare.CONTAINS : Compare.ENDS_WITH;
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return (operator1.hashOuter(hashContext) * 31 + operator2.hashOuter(hashContext)) * 31 + (compareType == null ? 0 : compareType);
    }

    @Override
    public boolean calcTwins(TwinImmutableObject obj) {
        return super.calcTwins(obj) && BaseUtils.nullEquals(compareType, ((LikeWhere)obj).compareType);
    }

    protected String getCompareSource(CompileSource compile) {
        throw new RuntimeException("not supported");
    }

    @Override
    protected String getBaseSource(CompileSource compile) {
        Type type = operator1.getType(compile.keyType);
        boolean needTrim = compareType != null && compareType.equals(3) && type instanceof StringClass && ((StringClass) type).blankPadded;
        String column = (needTrim ? "RTRIM(" : "") + operator1.getSource(compile) + (needTrim ? ")" : "");
        String likeString = type instanceof StringClass && ((StringClass) type).caseInsensitive ? " " + compile.syntax.getInsensitiveLike() + " " : " LIKE ";
        String before = compareType != null && (compareType.equals(2) || compareType.equals(3)) ? ("'%' " + compile.syntax.getStringConcatenate() + " ") : "";
        String after = compareType != null && (compareType.equals(1) || compareType.equals(2)) ? (" " + compile.syntax.getStringConcatenate() + " '%'") : "";
        return column + likeString + "(" + before + operator2.getSource(compile) + after + ")";
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2, Integer compareType) {
        if(checkEquals(operator1, operator2))
            return operator1.getWhere();
        return create(operator1, operator2, new LikeWhere(operator1, operator2, compareType));
    }
}
