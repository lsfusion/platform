package platform.server.where;

import platform.server.data.query.JoinData;
import platform.server.data.query.wheres.MapWhere;
import platform.base.BaseUtils;


abstract class ObjectWhere<Not extends ObjectWhere> extends AbstractWhere<Not> implements OrObjectWhere<Not>,AndObjectWhere<Not> {

    public Where pairs(AndObjectWhere pair, boolean plainFollow) {
        return null;
    }

    public boolean isTrue() {
        return false;
    }

    public boolean isFalse() {
        return false;
    }

    public AndObjectWhere[] getAnd() {
        return new AndObjectWhere[]{this};
    }

    public OrObjectWhere[] getOr() {
        return new OrObjectWhere[]{this};
    }

    public Where innerFollowFalse(Where falseWhere, boolean sureNotTrue) {
        // исходим из предположения что что !(not()=>falseWhere) то есть !(op,this,true).checkTrue
        if(!sureNotTrue && OrWhere.orTrue(this,falseWhere))
            return TRUE;
        if(means(falseWhere))
            return FALSE;
        else {
            Where result = packFollowFalse(falseWhere);
            if(!BaseUtils.hashEquals(this,result) && !sureNotTrue && OrWhere.orTrue(result,falseWhere)) // если упаковался еще раз на orTrue проверим
                return TRUE;
            return result;
        }
    }

    public Where packFollowFalse(Where falseWhere) {
        return this;
    }

    public boolean checkTrue() {
        return false;
    }

    public int getHeight() {
        return 1;
    }

    boolean depends(ObjectWhere where) {
        // собсно логика простая из элемента или его not'а должен следовать where элемент
        // потому как иначе положив все where = FALSE мы не изменим формулу никак
        return where.directMeansFrom(this) || where.directMeansFrom(not());
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillDataJoinWheres(joins,andWhere.and(this));
    }

    abstract protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere);
}
