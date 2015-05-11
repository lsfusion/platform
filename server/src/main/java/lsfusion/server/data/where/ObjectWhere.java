package lsfusion.server.data.where;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.innerjoins.KeyEquals;
import lsfusion.server.data.query.stat.KeyStat;


public abstract class ObjectWhere extends AbstractWhere implements OrObjectWhere<ObjectWhere>,AndObjectWhere<ObjectWhere> {

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

    public Where followFalse(CheckWhere falseWhere, boolean pack, FollowChange change) {
        if(OrWhere.checkTrue(not(),falseWhere)) {
            change.type = FollowType.NARROW;
            return FALSE;
        }
        if(pack) {
            Where result = packFollowFalse((Where)falseWhere);
            if(BaseUtils.hashEquals(this,result))
                return this;

            if(OrWhere.checkTrue(this,falseWhere)) { // проверим на checkTrue так как упакованный where уже другой, и соответственно проверка со "старым" where "потеряется", а это может привести к бесконечному проталкиванию и т.п.
                change.type = FollowType.WIDE;
                return TRUE;
            }
            if(OrWhere.checkTrue(result.not(),falseWhere)) {
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

    public void fillJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        fillDataJoinWheres(joins,andWhere.and(this));
    }

    abstract protected void fillDataJoinWheres(MMap<JoinData, Where> joins, Where andWhere);

    public KeyEquals calculateKeyEquals() {
        return new KeyEquals(this, true);  // в operator'ах никаких equals быть не может
    }

    public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        return new GroupJoinsWheres(this, type);
    }

    // обозначает что из getClassWhere => this where
    public boolean isClassWhere() {
        return false;
    }
}
