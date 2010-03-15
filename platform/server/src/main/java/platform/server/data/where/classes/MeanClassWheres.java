package platform.server.data.where.classes;

import platform.base.QuickMap;
import platform.server.caches.ManualLazy;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.Where;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.EqualMap;
import platform.server.data.expr.VariableClassExpr;

import java.util.Map;


public class MeanClassWheres extends DNFWheres<MeanClassWhere,MeanClassWheres> {

    protected boolean privateWhere() {
        return true;
    }
    protected MeanClassWheres createThis() {
        return new MeanClassWheres();
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
            if(!getValue(i).not().checkTrue()) { // если что можно на checkTrue переставить
                MeanClassWhere orMean = getKey(i);

                EqualMap equalMap = new EqualMap(orMean.equals.size()*2);
                for(Map.Entry<VariableClassExpr,VariableClassExpr> equal : orMean.equals.entrySet())
                    equalMap.add(equal.getKey(),equal.getValue());

                result = result.or(orMean.classWhere.andEquals(equalMap));
                assert !orMean.classWhere.means(getValue(i).not());
            }
        return result;
    }

    public MeanClassWheres() {
    }

    public MeanClassWheres(MeanClassWhere join,Where where) {
        add(join,where);
    }

    public MeanClassWheres translate(KeyTranslator translator) {
        MeanClassWheres result = new MeanClassWheres();
        for(int i=0;i<size;i++)
            result.add(getKey(i).translate(translator),getValue(i).translateDirect(translator));
        return result;
    }
}
