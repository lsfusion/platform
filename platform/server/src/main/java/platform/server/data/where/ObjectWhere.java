package platform.server.data.where;

import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.base.BaseUtils;


abstract class ObjectWhere extends AbstractWhere implements OrObjectWhere<ObjectWhere>,AndObjectWhere<ObjectWhere> {

    public abstract ObjectWhere not();

    public Where pairs(AndObjectWhere pair) {
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

    public Where followFalse(CheckWhere falseWhere, boolean sureNotTrue, boolean pack, FollowChange change) {
        // исходим из предположения что что !(not()=>falseWhere) то есть !(op,this,true).checkTrue
        if(!sureNotTrue && OrWhere.checkTrue(this,falseWhere)) {
            change.type = FollowType.WIDE;
            return TRUE;
        }
        if(OrWhere.checkTrue(not(),falseWhere)) {
            change.type = FollowType.NARROW;
            return FALSE;
        }
        if(pack) {
            Where result = packFollowFalse((Where)falseWhere);
            if(BaseUtils.hashEquals(this,result))
                return this;
            // если упаковался еще раз на orTrue проверим
            if(!sureNotTrue && OrWhere.checkTrue(this,falseWhere)) {
                change.type = FollowType.WIDE;
                return TRUE;
            }
            if(OrWhere.checkTrue(not(),falseWhere)) {
                change.type = FollowType.NARROW;
                return FALSE;
            }

            change.type = FollowType.DIFF;
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
