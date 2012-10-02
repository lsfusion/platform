package platform.server.data.where.classes;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.CheckWhere;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;


public class MeanClassWheres extends DNFWheres<MeanClassWhere, CheckWhere, MeanClassWheres> implements OuterContext<MeanClassWheres> {

    protected CheckWhere andValue(MeanClassWhere key, CheckWhere prevValue, CheckWhere newValue) {
        return prevValue.andCheck(newValue);
    }
    protected CheckWhere addValue(MeanClassWhere key, CheckWhere prevValue, CheckWhere newValue) {
        return prevValue.orCheck(newValue);
    }

    protected MeanClassWheres createThis() {
        return new MeanClassWheres();
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
        for(int i=0;i<size;i++)
            if(!getValue(i).not().checkTrue())
                result = result.or(getKey(i).getClassWhere());
        return result;
    }

    public MeanClassWheres() {
    }

    public MeanClassWheres(MeanClassWhere join,Where where) {
        add(join,where);
    }

    private MeanClassWheres(MeanClassWheres wheres, MapTranslate translator) {
        for(int i=0;i<size;i++)
            add(getKey(i).translateOuter(translator),getValue(i).translateOuter(translator));
    }

    public class OuterContext extends AbstractOuterContext<OuterContext> {
        protected OuterContext translate(MapTranslate translator) {
            return new MeanClassWheres(getThis(), translator).getOuter();
        }

        protected boolean isComplex() {
            return true;
        }

        protected int hash(HashContext hash) {
            int result = 0;
            for(int i=0;i<size;i++)
                result += getKey(i).hashOuter(hash) ^ ((Where)getValue(i)).hashOuter(hash);
            return result;
        }

        public QuickSet<platform.server.caches.OuterContext> calculateOuterDepends() {
            return new QuickSet<platform.server.caches.OuterContext>(keyIt()).merge(
                    new QuickSet<platform.server.caches.OuterContext>(BaseUtils.<Iterable<platform.server.caches.OuterContext>>immutableCast(valueIt())));
        }

        public MeanClassWheres getThis() {
            return MeanClassWheres.this;
        }
        public boolean twins(TwinImmutableInterface o) {
            return getThis().equals(((OuterContext)o).getThis());
        }
    }
    private OuterContext outer;
    public OuterContext getOuter() {
        if(outer==null)
            outer = new OuterContext();
        return outer;
    }
    public QuickSet<KeyExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public QuickSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public QuickSet<platform.server.caches.OuterContext> getOuterDepends() {
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
}
