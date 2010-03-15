package platform.server.data.where.classes;

import platform.server.data.expr.VariableClassExpr;
import platform.server.data.where.DNFWheres;
import platform.server.data.translator.KeyTranslator;
import platform.base.BaseUtils;

import java.util.Map;
import java.util.HashMap;

// не будем выделять общий функционал с InnerWhere потому как он весьма сомнительные
public class MeanClassWhere implements DNFWheres.Interface<MeanClassWhere> {

    public final static MeanClassWhere TRUE = new MeanClassWhere(ClassExprWhere.TRUE); 

    public final ClassExprWhere classWhere;
    public final Map<VariableClassExpr, VariableClassExpr> equals;

    public MeanClassWhere(ClassExprWhere classWhere) {
        this(classWhere, new HashMap<VariableClassExpr, VariableClassExpr>());
    }

    public MeanClassWhere(ClassExprWhere classWhere, Map<VariableClassExpr, VariableClassExpr> equals) {
        this.classWhere = classWhere;
        this.equals = equals;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof MeanClassWhere && classWhere.equals(((MeanClassWhere) o).classWhere) && equals.equals(((MeanClassWhere) o).equals);
    }

    @Override
    public int hashCode() {
        return 31 * classWhere.hashCode() + equals.hashCode();
    }

    // пока так потом компоненты надо образовывать будет
    public MeanClassWhere and(MeanClassWhere where) {
        return new MeanClassWhere(classWhere.and(where.classWhere), BaseUtils.merge(equals,where.equals)); // даже если совпадают ничего страшного, все равно зафиксировано в InnerJoins - Where
    }

    public boolean isFalse() {
        return false;
    }

    public MeanClassWhere translate(KeyTranslator translator) {
        Map<VariableClassExpr,VariableClassExpr> transEquals = new HashMap<VariableClassExpr, VariableClassExpr>();
        for(Map.Entry<VariableClassExpr,VariableClassExpr> equal : equals.entrySet())
            transEquals.put(equal.getKey().translateDirect(translator),equal.getValue().translateDirect(translator));
        return new MeanClassWhere(classWhere.translate(translator), transEquals);
    }

}
