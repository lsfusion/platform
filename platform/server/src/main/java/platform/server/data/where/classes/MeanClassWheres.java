package platform.server.data.where.classes;

import platform.base.QuickMap;
import platform.server.caches.ManualLazy;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.Where;


public class MeanClassWheres extends QuickMap<ClassExprWhere,Where> {

    public void or(MeanClassWheres joins) {
        addAll(joins);
    }

    protected Where addValue(Where prevValue, Where newValue) {
        return newValue.orMeans(prevValue);
    }

    protected boolean containsAll(Where who, Where what) {
        return what.means(who);
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
        for(int i=0;i<size;i++) {
            ClassExprWhere orMean = (ClassExprWhere) table[indexes[i]];
            if(!getValue(i).not().checkTrue()) { // если что можно на checkTrue переставить
                result = result.or(orMean);
                assert !orMean.means(getValue(i).not());
            }
        }
        return result;
    }

    public MeanClassWheres() {
    }

    @Override
    public boolean add(ClassExprWhere key, Where value) {
        assert (!key.isFalse());
        return super.add(key, value);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public MeanClassWheres(ClassExprWhere join,Where where) {
        add(join,where);
    }

    public MeanClassWheres and(MeanClassWheres joins) {
        MeanClassWheres result = new MeanClassWheres();
        // берем все пары joins'ов
        for(int i1=0;i1<size;i1++)
            for(int i2=0;i2<joins.size;i2++) {
                ClassExprWhere andJoin = getKey(i1).and(joins.getKey(i2));
                Where andWhere = getValue(i1).andMeans(joins.getValue(i2));
                if(!andJoin.isFalse())
                    result.add(andJoin, andWhere);
            }
        return result;
    }

    public MeanClassWheres translate(KeyTranslator translator) {
        MeanClassWheres result = new MeanClassWheres();
        for(int i=0;i<size;i++)
            result.add(getKey(i).translate(translator),getValue(i).translateDirect(translator));
        return result;
    }
}
