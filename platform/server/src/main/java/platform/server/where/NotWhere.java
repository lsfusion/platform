package platform.server.where;

import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.MeanClassWheres;
import platform.server.data.query.*;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;

import java.util.Map;

class NotWhere extends ObjectWhere<DataWhere> {

    DataWhere where;
    NotWhere(DataWhere iWhere) {
        where = iWhere;
    }

    public ObjectWhereSet calculateObjects() {
        return new ObjectWhereSet(this);
    }

    public boolean directMeansFrom(AndObjectWhere meanWhere) {
        return meanWhere instanceof NotWhere && where.follow(((NotWhere)meanWhere).where);
    }

    public DataWhere getNot() {
        return where;
    }

    final static String PREFIX = "NOT ";
    public String toString() {
        return PREFIX+where;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof NotWhere && where.equals(((NotWhere)o).where);
    }

    protected int getHashCode() {
        return where.hashCode()*31;
    }

    public Where decompose(ObjectWhereSet decompose, ObjectWhereSet objects) {
        if(where.getFollows().intersect(decompose.data))
            return TRUE;
        else {
            objects.not.add(where);
            objects.followNot.addAll(where.getFollows());
            return this;
        }
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public Where translate(Translator translator) {
        return where.translate(translator).not();
    }


    public int fillContext(Context context, boolean compile) {
        return where.fillContext(context, compile);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return where.getNotSource(queryData,syntax);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        where.fillDataJoinWheres(joins, andWhere);
    }

    public InnerJoins getInnerJoins() {
        return new InnerJoins(Where.TRUE,this);
    }

    public MeanClassWheres getMeanClassWheres() {
        return new MeanClassWheres(ClassExprWhere.TRUE,this);
    }

    public boolean equals(Where equalWhere, MapContext mapContext) {
        return equalWhere instanceof NotWhere && where.equals(((NotWhere)equalWhere).where, mapContext) ;
    }

    protected int getHash() {
        return where.hash()*3;
    }

    public ClassExprWhere calculateClassWhere() {
        return ClassExprWhere.TRUE;
    }

    @Override
    public Where linearFollowFalse(Where falseWhere) {
        return where.linearFollowFalse(falseWhere).not();
    }
}
