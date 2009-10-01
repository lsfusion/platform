package platform.server.where;

import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.classes.where.MeanClassWheres;
import platform.server.data.query.*;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.wheres.MapWhere;

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

    public DataWhere calculateNot() {
        return where;
    }

    final static String PREFIX = "NOT ";

    public boolean twins(AbstractSourceJoin o) {
        return where.equals(((NotWhere)o).where);
    }

    public int hashContext(HashContext hashContext) {
        return where.hashContext(hashContext)*31;
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

    public Where translateDirect(KeyTranslator translator) {
        return where.translateDirect(translator).not();
    }
    public Where translateQuery(QueryTranslator translator) {
        return where.translateQuery(translator).not();
    }

    public void fillContext(Context context) {
        where.fillContext(context);
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return PREFIX + where.getSource(compile);

        return where.getNotSource(compile);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        where.fillDataJoinWheres(joins, andWhere);
    }

    public InnerJoins getInnerJoins() {
        return new InnerJoins(this);
    }

    public MeanClassWheres calculateMeanClassWheres() {
        return new MeanClassWheres(ClassExprWhere.TRUE,this);
    }

    public ClassExprWhere calculateClassWhere() {
        return ClassExprWhere.TRUE;
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        return where.packFollowFalse(falseWhere).not();
    }
}
