package platform.server.data.where.classes;

import platform.server.caches.ManualLazy;
import platform.server.data.expr.VariableClassExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.CheckWhere;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.EqualMap;
import platform.server.data.where.Where;

import java.util.Map;


public class MeanClassWheres extends DNFWheres<MeanClassWhere, CheckWhere, MeanClassWheres> {

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
            if(!getValue(i).not().checkTrue()) { // если что можно на checkTrue переставить
                MeanClassWhere orMean = getKey(i);

                EqualMap equalMap = new EqualMap(orMean.equals.size()*2);
                for(Map.Entry<VariableClassExpr,VariableClassExpr> equal : orMean.equals.entrySet())
                    equalMap.add(equal.getKey(),equal.getValue());

                result = result.or(orMean.classWhere.andEquals(equalMap));
//                assert !orMean.classWhere.means(getValue(i).not());
            }
        return result;
    }

    public MeanClassWheres() {
    }

    public MeanClassWheres(MeanClassWhere join,Where where) {
        add(join,where);
    }

    public MeanClassWheres translateOuter(MapTranslate translator) {
        MeanClassWheres result = new MeanClassWheres();
        for(int i=0;i<size;i++)
            result.add(getKey(i).translate(translator),getValue(i).translateOuter(translator));
        return result;
    }
}
