package lsfusion.server.data.where.classes;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.StaticValueExpr;
import lsfusion.server.data.query.ExprEnumerator;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.AbstractWhere;
import lsfusion.server.data.where.CheckWhere;
import lsfusion.server.data.where.DNFWheres;
import lsfusion.server.data.where.Where;


public class MeanClassWheres extends DNFWheres<MeanClassWhere, CheckWhere, MeanClassWheres> implements OuterContext<MeanClassWheres> {

    protected AddValue<MeanClassWhere, CheckWhere> getAddValue() {
        return AbstractWhere.addOrCheck();
    }

    protected CheckWhere andValue(MeanClassWhere key, CheckWhere prevValue, CheckWhere newValue) {
        return prevValue.andCheck(newValue);
    }

    protected MeanClassWheres createThis(ImMap<MeanClassWhere, CheckWhere> map) {
        return new MeanClassWheres(map);
    }

    @Override
    protected boolean valueIsFalse(CheckWhere value) {
        return value.isFalse();
    }

    ClassExprWhere classWhere;
    @ManualLazy
    public ClassExprWhere getClassWhere() {
        if(classWhere==null)
            classWhere = calculateClassWhere();
        return classWhere;
    }

    public ClassExprWhere calculateClassWhere() {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(int i=0,size=size();i<size;i++)
            if(!getValue(i).not().checkTrue())
                result = result.or(getKey(i).getClassWhere());
        return result;
    }

    public MeanClassWheres(ImMap<MeanClassWhere, CheckWhere> map) {
        super(map);
    }

    public MeanClassWheres(MeanClassWhere join,Where where) {
        super(join, where);
    }

    public class OuterContext extends AbstractOuterContext<OuterContext> {
        protected OuterContext translate(MapTranslate translator) {
            return new MeanClassWheres(translator.translateMap(map)).getOuter();
        }

        protected boolean isComplex() {
            return true;
        }

        protected int hash(HashContext hash) {
            int result = 0;
            for(int i=0,size=size();i<size;i++)
                result += getKey(i).hashOuter(hash) ^ ((Where)getValue(i)).hashOuter(hash);
            return result;
        }

        public ImSet<lsfusion.server.caches.OuterContext> calculateOuterDepends() {
            return SetFact.<lsfusion.server.caches.OuterContext>merge(keys(), BaseUtils.<ImSet<OuterContext>>immutableCast(values().toSet()));
        }

        public MeanClassWheres getThis() {
            return MeanClassWheres.this;
        }
        public boolean twins(TwinImmutableObject o) {
            return getThis().equals(((OuterContext)o).getThis());
        }
    }
    private OuterContext outer;
    public OuterContext getOuter() {
        if(outer==null)
            outer = new OuterContext();
        return outer;
    }
    public ImSet<ParamExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public ImSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public ImSet<lsfusion.server.caches.OuterContext> getOuterDepends() {
        return getOuter().getOuterDepends();
    }
    public boolean enumerate(ExprEnumerator enumerator) {
        return getOuter().enumerate(enumerator);
    }
    public long getComplexity(boolean outer) {
        return getOuter().getComplexity(outer);
    }
    public MeanClassWheres translateOuter(MapTranslate translator) {
        return getOuter().translateOuter(translator).getThis();
    }
    public MeanClassWheres pack() {
        throw new RuntimeException("not supported yet");
    }
    public ImSet<StaticValueExpr> getOuterStaticValues() {
        throw new RuntimeException("should not be");
    }
}
