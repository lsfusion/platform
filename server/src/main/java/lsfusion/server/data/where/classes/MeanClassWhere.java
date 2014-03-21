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
    private final ImSet<ImSet<VariableClassExpr>> greaters; // нужны для выведения классов для случаев a<5

    public ClassExprWhere getClassWhere(BaseExpr operator1, BaseExpr operator2, boolean isEquals) {
        assert classNotWhere.isFalse();
        if(operator1 instanceof VariableClassExpr && operator2 instanceof VariableClassExpr) {
            assert equals.size() + greaters.size() == 1 && ((equals.size()==1)==isEquals);
            EqualMap equalMap = new EqualMap(2);
            equalMap.add(operator1,operator2);
            return classWhere.andEquals(equalMap, !isEquals);
        } else {
            assert equals.size() + greaters.size() == 0;
            return classWhere;
        }
    }

    private static int getCompSize(ImSet<ImSet<VariableClassExpr>> comps) {
        int result = 0;
        for(int i=0;i<comps.size();i++)
            result += comps.get(i).size();
        return result;
    }

    private static void fillEqualMap(ImSet<ImSet<VariableClassExpr>> comps, EqualMap equalMap) { // избыточное выполнение, но пока не важно
        for(int i=0;i<comps.size();i++) {
            ImSet<VariableClassExpr> equal = comps.get(i);
            VariableClassExpr firstEqual = equal.get(0);
            for(int j=1;j<equal.size();j++)
                equalMap.add(firstEqual, equal.get(j));
        }
    }

    public ClassExprWhere getClassWhere() {
        EqualMap equalMap = new EqualMap((getCompSize(equals) + getCompSize(greaters)) *2);

        ClassExprWhere eqClassWhere = classWhere;
        ClassExprWhere eqClassNotWhere = classNotWhere;

        fillEqualMap(equals, equalMap); // сливаем equals
        eqClassWhere = eqClassWhere.andEquals(equalMap);
        eqClassNotWhere = eqClassNotWhere.andEquals(equalMap);

        fillEqualMap(greaters, equalMap); // тут могла быть проблема с staticExpr'ами, но здесь она изначально решена, так как и equals и greaters работают с VariableClassExpr
        eqClassWhere = eqClassWhere.andEquals(equalMap, true);
        eqClassNotWhere = eqClassNotWhere.andEquals(equalMap, true);

        return eqClassWhere.andNot(eqClassNotWhere);
    }

    public MeanClassWhere(ClassExprWhere classWhere) {
        this(classWhere, SetFact.<ImSet<VariableClassExpr>>EMPTY(), true);
    }
    
    public MeanClassWhere(ClassExprWhere classWhere, boolean not) {
        this(ClassExprWhere.TRUE, classWhere, SetFact.<ImSet<VariableClassExpr>>EMPTY(), SetFact.<ImSet<VariableClassExpr>>EMPTY());
        assert not;
    }

    public MeanClassWhere(ClassExprWhere classWhere, ImSet<ImSet<VariableClassExpr>> comps, boolean isEquals) {
        this(classWhere, ClassExprWhere.FALSE, isEquals ? comps : SetFact.<ImSet<VariableClassExpr>>EMPTY(), isEquals ? SetFact.<ImSet<VariableClassExpr>>EMPTY() : comps);
    }

    public MeanClassWhere(ClassExprWhere classWhere, ClassExprWhere classNotWhere, ImSet<ImSet<VariableClassExpr>> equals, ImSet<ImSet<VariableClassExpr>> greaters) {
        this.classWhere = classWhere;
        this.classNotWhere = classNotWhere;
        this.equals = equals;
        this.greaters = greaters;
    }

    // пока так потом компоненты надо образовывать будет
    public MeanClassWhere and(MeanClassWhere where) {
        return new MeanClassWhere(classWhere.and(where.classWhere), classNotWhere.or(where.classNotWhere), andComps(equals, where.equals), andComps(greaters, where.greaters));
    }

    private static ImSet<ImSet<VariableClassExpr>> andComps(ImSet<ImSet<VariableClassExpr>> comps, ImSet<ImSet<VariableClassExpr>> whereComps) {
        EqualMap equalMap = new EqualMap((getCompSize(comps) + getCompSize(whereComps)) * 2);
        fillEqualMap(comps, equalMap);
        fillEqualMap(whereComps, equalMap);

        MSet<ImSet<VariableClassExpr>> mAndEquals = SetFact.mSet();
        for(int i=0;i<equalMap.num;i++) {
            Equal equal = equalMap.comps[i];
            if(!equal.dropped)
                mAndEquals.add(BaseUtils.<ImSet<VariableClassExpr>>immutableCast(SetFact.toExclSet(equal.size, equal.exprs)));
        }
        return mAndEquals.immutable();
    }

    public MeanClassWhere translate(final MapTranslate translator) {
        return new MeanClassWhere(classWhere.translateOuter(translator), classNotWhere.translateOuter(translator), translateComps(translator, equals), translateComps(translator, greaters));
    }

    private ImSet<ImSet<VariableClassExpr>> translateComps(final MapTranslate translator, ImSet<ImSet<VariableClassExpr>> comps) {
        return comps.mapSetValues(new GetValue<ImSet<VariableClassExpr>, ImSet<VariableClassExpr>>() {
                public ImSet<VariableClassExpr> getMapValue(ImSet<VariableClassExpr> value) {
                    return translator.translateVariable(value);
                }
            });
    }

    protected int hash(HashContext hash) {
        return 31 * (31 * (classWhere.hashOuter(hash) * 31 + classNotWhere.hashOuter(hash)) + hashComps(hash, equals)) + hashComps(hash, greaters);
    }

    private static int hashComps(HashContext hash, ImSet<ImSet<VariableClassExpr>> comps) {
        int result = 0;
        for(int i=0,size= comps.size();i<size;i++)
            result ^= AbstractOuterContext.hashOuter(comps.get(i), hash);
        return result;
    }

    protected boolean isComplex() {
        return true;
    }

    public ImSet<OuterContext> calculateOuterDepends() {
        return classWhere.getOuterDepends().merge(classNotWhere.getOuterDepends()).merge(getOuterDepends(equals)).merge(getOuterDepends(greaters));
    }

    private static ImSet<OuterContext> getOuterDepends(ImSet<ImSet<VariableClassExpr>> comps) {
        MSet<OuterContext> mEqualContext = SetFact.mSet();
        for(int i=0,size= comps.size();i<size;i++)
            mEqualContext.addAll(comps.get(i));
        return mEqualContext.immutable();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return classWhere.equals(((MeanClassWhere) o).classWhere) && classNotWhere.equals(((MeanClassWhere) o).classNotWhere) && equals.equals(((MeanClassWhere) o).equals) && greaters.equals(((MeanClassWhere) o).greaters);
    }

    public String toString() {
        return classWhere.toString() + " N " + classNotWhere.toString() + " " + equals.toString() + " " + greaters.toString();
    }
}
