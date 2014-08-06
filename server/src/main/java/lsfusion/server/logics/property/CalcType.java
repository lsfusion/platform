package lsfusion.server.logics.property;

import lsfusion.server.logics.property.infer.InferType;

public class CalcType {
    protected CalcType() {
    }

    public final static CalcType EXPR = new CalcType(); // вычисления
    public final static CalcType STAT = new CalcType(); // определение статистики, пока нигде не используется, кроме того чтобы предотвратить кэширования, когда статистика еще не обновлена

    public boolean isStat() {
        return this == STAT;
    }

    public boolean isExpr() {
        return this == EXPR; 
    }

    public AlgInfoType getAlgInfo() {
        return AlgType.useInferForInfo ? InferType.PREVBASE : CalcClassType.PREVBASE;
    }
}
