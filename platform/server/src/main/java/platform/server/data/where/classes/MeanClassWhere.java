package platform.server.data.where.classes;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.VariableClassExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Equal;
import platform.server.data.where.EqualMap;

import java.util.HashMap;
import java.util.Map;

// не будем выделять общий функционал с InnerWhere потому как он весьма сомнительные
public class MeanClassWhere extends AbstractOuterContext<MeanClassWhere> implements DNFWheres.Interface<MeanClassWhere> {

    public final static MeanClassWhere TRUE = new MeanClassWhere(ClassExprWhere.TRUE); 

    private final ClassExprWhere classWhere;
    private final QuickSet<QuickSet<VariableClassExpr>> equals;

    public ClassExprWhere getClassWhere(BaseExpr operator1, BaseExpr operator2) {
        if(operator1 instanceof VariableClassExpr && operator2 instanceof VariableClassExpr) {
            assert equals.size==1;
            EqualMap equalMap = new EqualMap(2);
            equalMap.add(operator1,operator2);
            return classWhere.andEquals(equalMap);
        } else {
            assert equals.size==0;
            return classWhere;
        }
    }

    private int getEqualSize() {
        int result = 0;
        for(int i=0;i<equals.size;i++)
            result += equals.get(i).size;
        return result;
    }
    private void fillEqualMap(EqualMap equalMap) { // избыточное выполнение, но пока не важно
        for(int i=0;i<equals.size;i++) {
            QuickSet<VariableClassExpr> equal = equals.get(i);
            VariableClassExpr firstEqual = equal.get(0);
            for(int j=1;j<equal.size;j++)
                equalMap.add(firstEqual, equal.get(j));
        }
    }
    
    public ClassExprWhere getClassWhere() {
        EqualMap equalMap = new EqualMap(getEqualSize()*2);
        fillEqualMap(equalMap);
        return classWhere.andEquals(equalMap);
    }

    public MeanClassWhere(ClassExprWhere classWhere) {
        this(classWhere, new QuickSet<QuickSet<VariableClassExpr>>());
    }

    public MeanClassWhere(ClassExprWhere classWhere, QuickSet<QuickSet<VariableClassExpr>> equals) {
        this.classWhere = classWhere;
        this.equals = equals;
    }

    // пока так потом компоненты надо образовывать будет
    public MeanClassWhere and(MeanClassWhere where) {
        EqualMap equalMap = new EqualMap((getEqualSize() + where.getEqualSize()) * 2);
        fillEqualMap(equalMap); where.fillEqualMap(equalMap);

        QuickSet<QuickSet<VariableClassExpr>> andEquals = new QuickSet<QuickSet<VariableClassExpr>>();
        for(int i=0;i<equalMap.num;i++) {
            Equal equal = equalMap.comps[i];
            if(!equal.dropped)
                andEquals.add(new QuickSet<VariableClassExpr>(equal.size, equal.exprs));
        }
        return new MeanClassWhere(classWhere.and(where.classWhere), andEquals);
    }

    public MeanClassWhere translate(MapTranslate translator) {
        QuickSet<QuickSet<VariableClassExpr>> transEquals = new QuickSet<QuickSet<VariableClassExpr>>();
        for(int i=0;i<equals.size;i++)
            transEquals.add(translator.translateVariable(equals.get(i)));
        return new MeanClassWhere(classWhere.translateOuter(translator), transEquals);
    }

    protected int hash(HashContext hash) {
        int result = 0;
        for(int i=0;i<equals.size;i++)
            result ^= AbstractOuterContext.hashOuter(equals.get(i), hash);
        return classWhere.hashOuter(hash) * 31 + result;
    }

    protected boolean isComplex() {
        return true;
    }

    public QuickSet<OuterContext> calculateOuterDepends() {
        QuickSet<OuterContext> equalContext = new QuickSet<OuterContext>();
        for(int i=0;i<equals.size;i++)
            equalContext.addAll(equals.get(i));
        return classWhere.getOuterDepends().merge(equalContext);
    }

    public boolean twins(TwinImmutableInterface o) {
        return classWhere.equals(((MeanClassWhere) o).classWhere) && equals.equals(((MeanClassWhere) o).equals);
    }

    public String toString() {
        return classWhere.toString() + " " + equals.toString();
    }
}
