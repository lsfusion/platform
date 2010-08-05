package platform.server.data.where;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.ObjectJoinSets;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWhere;
import platform.server.data.where.classes.MeanClassWheres;

public class NotWhere extends ObjectWhere {

    DataWhere where;
    NotWhere(DataWhere iWhere) {
        where = iWhere;
    }

    public boolean directMeansFrom(AndObjectWhere meanWhere) {
        for(OrObjectWhere orWhere : meanWhere.getOr())
            if(orWhere instanceof NotWhere && where.follow(((NotWhere)orWhere).where))
                return true;
        return false;
    }

    public DataWhere not() {
        return where;
    }

    final static String PREFIX = "NOT ";

    public boolean twins(AbstractSourceJoin o) {
        return where.equals(((NotWhere)o).where);
    }

    public int hashOuter(HashContext hashContext) {
        return where.hashOuter(hashContext)*31;
    }

    // ДОПОЛНИТЕЛЬНЫЕ ИНТЕРФЕЙСЫ

    public Where translateOuter(MapTranslate translator) {
        return where.translateOuter(translator).not();
    }
    public Where translateQuery(QueryTranslator translator) {
        return where.translateQuery(translator).not();
    }

    public void enumerate(ContextEnumerator enumerator) {
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

    public ObjectJoinSets groupObjectJoinSets() {
        return new ObjectJoinSets(this);
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
