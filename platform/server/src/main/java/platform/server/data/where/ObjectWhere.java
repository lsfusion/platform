package platform.server.data.where;

import platform.base.BaseUtils;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.KeyEquals;


abstract class ObjectWhere extends AbstractWhere implements OrObjectWhere<ObjectWhere>,AndObjectWhere<ObjectWhere> {

    public abstract ObjectWhere not();

    public Where pairs(AndObjectWhere pair, FollowDeep followDeep) {
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

    public Where innerFollowFalse(Where falseWhere, boolean sureNotTrue, boolean packExprs) {
        // исходим из предположения что что !(not()=>falseWhere) то есть !(op,this,true).checkTrue
        if(!sureNotTrue && OrWhere.orTrue(this,falseWhere))
            return TRUE;
        if(means(falseWhere))
            return FALSE;
        if(packExprs) {
            Where result = packFollowFalse(falseWhere);
            if(BaseUtils.hashEquals(this,result))
                return this;
            else
                if(OrWhere.orTrue(result,falseWhere)) // если упаковался еще раз на orTrue проверим
                    return TRUE;
                else
                    return result;
        }
        return this;
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

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillDataJoinWheres(joins,andWhere.and(this));
    }

    abstract protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public KeyEquals groupKeyEquals() {
        return new KeyEquals(this);  // в operator'ах никаких equals быть не может
    }
}
