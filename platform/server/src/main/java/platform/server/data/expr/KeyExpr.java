package platform.server.data.expr;

import platform.server.classes.BaseClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.expr.where.IsClassWhere;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;
import platform.server.caches.HashContext;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;

public class KeyExpr extends VariableClassExpr {

    public static <T> Map<T, KeyExpr> getMapKeys(Collection<T> objects) {
        Map<T,KeyExpr> result = new HashMap<T, KeyExpr>();
        for(T object : objects)
            result.put(object,new KeyExpr(object.toString()));
        return result;
    }

    final String name;
    @Override
    public String toString() {
        return name;
    }

    public KeyExpr(String name) {
        this.name = name;
    }

    public String getSource(CompileSource compile) {
        assert compile.keySelect.containsKey(this);
        return compile.keySelect.get(this);
    }

    public void enumerate(SourceEnumerator enumerator) {
        enumerator.add(this);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Type getType(Where where) {
        return where.getClassWhere().getType(this);
    }

    // возвращает Where без следствий
    public Where calculateWhere() {
        return Where.TRUE;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return translator.translate(this);
    }

    public KeyExpr translateDirect(KeyTranslator translator) {
        return translator.translate(this);
    }

    public DataWhereSet getFollows() {
        return new DataWhereSet();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public int hashContext(HashContext hashContext) {
        return hashContext.hash(this);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return false;
    }
}
