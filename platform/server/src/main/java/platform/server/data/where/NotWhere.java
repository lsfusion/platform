package platform.server.data.where;

import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;
import platform.server.data.where.classes.MeanClassWhere;
import platform.server.data.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.MapWhere;
import platform.server.caches.hash.HashContext;

public class NotWhere extends ObjectWhere {

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

    public DataWhere not() {
        return where;
    }

    final static String PREFIX = "NOT ";

    public boolean twins(AbstractSourceJoin o) {
        return where.equals(((NotWhere)o).where);
    }

    public int hashContext(HashContext hashContext) {
        return where.hashContext(hashContext)*31;
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public Where translateDirect(KeyTranslator translator) {
        return where.translateDirect(translator).not();
    }
    public Where translateQuery(QueryTranslator translator) {
        return where.translateQuery(translator).not();
    }

    public void enumerate(SourceEnumerator enumerator) {
        where.enumerate(enumerator);
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
        return new MeanClassWheres(MeanClassWhere.TRUE,this);
    }

    public ClassExprWhere calculateClassWhere() {
        return ClassExprWhere.TRUE;
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        return where.packFollowFalse(falseWhere).not();
    }
}
