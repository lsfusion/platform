package lsfusion.server.logics.property;

import lsfusion.server.logics.property.infer.InferType;

public class CalcType {
    private final String caption;
    protected CalcType(String caption) {
        this.caption = caption;
    }

    @Override
    public String toString() {
        return caption;
    }

    public final static CalcType EXPR = new CalcType("EXPR"); // вычисления
    public final static CalcType STAT = new CalcType("STAT"); // определение статистики, пока нигде не используется, кроме того чтобы предотвратить кэширования, когда статистика еще не обновлена
    public final static CalcType RECALC = new CalcType("RECALC"); // перерасчет inconsistent для FULL свойств

    public boolean isStat() {
        return this == STAT;
    }

    public boolean isExpr() {
        return this == EXPR; 
    }

    public boolean isRecalc() {
        return this == RECALC;
    }

    public AlgInfoType getAlgInfo() {
        return AlgType.useInferForInfo ? InferType.PREVBASE : CalcClassType.PREVBASE;
    }
}
