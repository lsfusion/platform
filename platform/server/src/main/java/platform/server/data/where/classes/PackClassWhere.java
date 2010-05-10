package platform.server.data.where.classes;

import platform.server.data.query.*;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.where.DataWhere;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.caches.hash.HashContext;

// упрощенный Where
public class PackClassWhere extends DataWhere {

    ClassExprWhere packWhere;

    public PackClassWhere(ClassExprWhere packWhere) {
        this.packWhere = packWhere;

        assert !packWhere.isFalse();
        assert !packWhere.isTrue();
    }

    protected DataWhereSet getExprFollows() {
        return packWhere.getFollows();
    }

    public void enumerate(SourceEnumerator enumerator) {
//        throw new RuntimeException("Not supported");
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        throw new RuntimeException("Not supported");
    }

    public int hashContext(HashContext hashContext) {
        return System.identityHashCode(this);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return false;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return packWhere.toString();

        throw new RuntimeException("Not supported");
    }

    @Override
    public String toString() {
        return packWhere.toString();
    }

    public Where translateDirect(DirectTranslator translator) {
        throw new RuntimeException("Not supported");
    }
    public Where translateQuery(QueryTranslator translator) {
        throw new RuntimeException("Not supported");
    }

    public InnerJoins getInnerJoins() {
        throw new RuntimeException("Not supported");
    }
    public ClassExprWhere calculateClassWhere() {
        return packWhere;
    }
}
