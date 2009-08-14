package platform.server.data.classes.where;

import platform.base.QuickMap;
import platform.server.where.Where;


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

    public ClassExprWhere orMeans() {
        ClassExprWhere result = ClassExprWhere.FALSE;
        for(int i=0;i<size;i++) {
            ClassExprWhere orMean = (ClassExprWhere) table[indexes[i]];
            if(!orMean.means(getValue(i).not()))
                result = result.or(orMean);
/*            else
                if(!vtable[indexes[i]].isFalse())
                    throw new RuntimeException("found");*/
        }
        return result;
    }

    public MeanClassWheres() {
    }

    public MeanClassWheres(ClassExprWhere join,Where where) {
        assert (!join.isFalse());
        add(join,where);
    }

    public MeanClassWheres and(MeanClassWheres joins) {
        MeanClassWheres result = new MeanClassWheres();
        // берем все пары joins'ов
        for(int i1=0;i1<size;i1++)
            for(int i2=0;i2<joins.size;i2++) {
                ClassExprWhere andJoin = ((ClassExprWhere)table[indexes[i1]]).and((ClassExprWhere)joins.table[joins.indexes[i2]]);
                Where andWhere = getValue(i1).andMeans(joins.getValue(i2));
                if(!andJoin.isFalse())
                    result.add(andJoin, andWhere);
            }
        return result;
    }
}
