package lsfusion.server.logics.property;

import lsfusion.server.logics.property.classes.infer.AlgInfoType;
import lsfusion.server.logics.property.classes.infer.AlgType;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.classes.infer.InferType;

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
    public final static CalcType STAT_ALOT = new CalcType("STAT_HASALOT"); // определение статистики для hasAlotKeys
    public final static CalcType RECALC = new CalcType("RECALC"); // перерасчет inconsistent для FULL свойств

    public boolean isStatAlot() {
        return this == STAT_ALOT;
    }

    public boolean isExpr() {
        return this == EXPR; 
    }

    public boolean isRecalc() {
        return this == RECALC;
    }

    public AlgInfoType getAlgInfo() {
        return AlgType.useInferForInfo ? InferType.prevBase() : CalcClassType.prevBase();
    }
}
