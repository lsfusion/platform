package lsfusion.server.data.where.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.TranslateContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.VariableClassExpr;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Equal;
import lsfusion.server.data.where.EqualMap;

// не будем выделять общий функционал с InnerWhere потому как он весьма сомнительные
public class MeanClassWhere extends AbstractOuterContext<MeanClassWhere> implements DNFWheres.Interface<MeanClassWhere>, TranslateContext<MeanClassWhere> {

    public final static MeanClassWhere TRUE = new MeanClassWhere(ClassExprWhere.TRUE); 

    private final ClassExprWhere classWhere;
    private final ClassExprWhere classNotWhere;
    private final ImSet<ImSet<VariableClassExpr>> equals;

    public ClassExprWhere getClassWhere(BaseExpr operator1, BaseExpr operator2) {
        assert classNotWhere.isFalse();
        if(operator1 instanceof VariableClassExpr && operator2 instanceof VariableClassExpr) {
            assert equals.size()==1;
            EqualMap equalMap = new EqualMap(2);
            equalMap.add(operator1,operator2);
            return classWhere.andEquals(equalMap);
        } else {
            assert equals.size()==0;
            return classWhere;
        }
    }

    private int getEqualSize() {
        int result = 0;
        for(int i=0;i<equals.size();i++)
            result += equals.get(i).size();
        return result;
    }
    private void fillEqualMap(EqualMap equalMap) { // избыточное выполнение, но пока не важно
        for(int i=0;i<equals.size();i++) {
            ImSet<VariableClassExpr> equal = equals.get(i);
            VariableClassExpr firstEqual = equal.get(0);
            for(int j=1;j<equal.size();j++)
                equalMap.add(firstEqual, equal.get(j));
        }
    }
    
    public ClassExprWhere getClassWhere() {
        EqualMap equalMap = new EqualMap(getEqualSize()*2);
        fillEqualMap(equalMap);
        return classWhere.andEquals(equalMap).andNot(classNotWhere.andEquals(equalMap));
    }

    public MeanClassWhere(ClassExprWhere classWhere) {
        this(classWhere, SetFact.<ImSet<VariableClassExpr>>EMPTY());
    }
    
    public MeanClassWhere(ClassExprWhere classWhere, boolean not) {
        this(ClassExprWhere.TRUE, classWhere, SetFact.<ImSet<VariableClassExpr>>EMPTY());
        assert not;
    }

    public MeanClassWhere(ClassExprWhere classWhere, ImSet<ImSet<VariableClassExpr>> equals) {
        this(classWhere, ClassExprWhere.FALSE, equals);
    }

    public MeanClassWhere(ClassExprWhere classWhere, ClassExprWhere classNotWhere, ImSet<ImSet<VariableClassExpr>> equals) {
        this.classWhere = classWhere;
        this.classNotWhere = classNotWhere;
        this.equals = equals;
    }

    // пока так потом компоненты надо образовывать будет
    public MeanClassWhere and(MeanClassWhere where) {
        EqualMap equalMap = new EqualMap((getEqualSize() + where.getEqualSize()) * 2);
        fillEqualMap(equalMap); where.fillEqualMap(equalMap);

        MSet<ImSet<VariableClassExpr>> mAndEquals = SetFact.mSet();
        for(int i=0;i<equalMap.num;i++) {
            Equal equal = equalMap.comps[i];
            if(!equal.dropped)
                mAndEquals.add(BaseUtils.<ImSet<VariableClassExpr>>immutableCast(SetFact.toExclSet(equal.size, equal.exprs)));
        }
        return new MeanClassWhere(classWhere.and(where.classWhere), classNotWhere.or(where.classNotWhere), mAndEquals.immutable());
    }

    public MeanClassWhere translate(final MapTranslate translator) {
        ImSet<ImSet<VariableClassExpr>> transEquals = equals.mapSetValues(new GetValue<ImSet<VariableClassExpr>, ImSet<VariableClassExpr>>() {
            public ImSet<VariableClassExpr> getMapValue(ImSet<VariableClassExpr> value) {
                return translator.translateVariable(value);
            }});
        return new MeanClassWhere(classWhere.translateOuter(translator), classNotWhere.translateOuter(translator), transEquals);
    }

    protected int hash(HashContext hash) {
        int result = 0;
        for(int i=0,size=equals.size();i<size;i++)
            result ^= AbstractOuterContext.hashOuter(equals.get(i), hash);
        return 31 * (classWhere.hashOuter(hash) * 31 + classNotWhere.hashOuter(hash)) + result;
    }

    protected boolean isComplex() {
        return true;
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        MSet<OuterContext> mEqualContext = SetFact.mSet();
        for(int i=0,size=equals.size();i<size;i++)
            mEqualContext.addAll(equals.get(i));
        return classWhere.getOuterDepends().merge(classNotWhere.getOuterDepends()).merge(mEqualContext.immutable());
    }

    public boolean twins(TwinImmutableObject o) {
        return classWhere.equals(((MeanClassWhere) o).classWhere) && classNotWhere.equals(((MeanClassWhere) o).classNotWhere) && equals.equals(((MeanClassWhere) o).equals);
    }

    public String toString() {
        return classWhere.toString() + " N " + classNotWhere.toString() + " " + equals.toString();
    }
}
