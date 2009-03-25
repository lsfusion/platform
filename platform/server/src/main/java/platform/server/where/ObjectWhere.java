package platform.server.where;

import platform.server.data.query.JoinData;
import platform.server.data.query.wheres.MapWhere;
import net.jcip.annotations.Immutable;


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

    public Where siblingsFollow(Where falseWhere) {
        if(OrWhere.op(not(),falseWhere,true).checkTrue())
            return FALSE;
        else
            return this;
    }

    public boolean checkTrue() {
        return false;
    }

    public int getSize() {
        return 1;
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
