package platform.server.where;

import java.util.Collection;
import java.util.Map;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.query.QueryData;

abstract public class DataWhere extends ObjectWhere<NotWhere> {

    // определяет все
    protected abstract DataWhereSet getExprFollows();

    public boolean directMeansFrom(AndObjectWhere where) {
        return where instanceof DataWhere && ((DataWhere)where).follow(this);
    }

    public NotWhere getNot() {
        return new NotWhere(this);
    }

    public boolean follow(DataWhere dataWhere) {
        return getFollows().contains(dataWhere);
    }

    public ObjectWhereSet calculateObjects() {
        return new ObjectWhereSet(this);
    }

    public Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects) {
        if(decompose.followNot.contains(this))
            return TRUE;
        else {
            objects.data.add(this);
            objects.followData.addAll(getFollows());
            return this;
        }
    }

    // возвращает себя и все зависимости
    DataWhereSet follows = null;
    public DataWhereSet getFollows() {
        if(follows==null) {
            follows = new DataWhereSet(getExprFollows());
            follows.add(this);
        }
        return follows;
    }

    public boolean evaluate(Collection<DataWhere> data) {
        return data.contains(this);
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    protected String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return NotWhere.PREFIX + getSource(queryData, syntax);
    }
}
