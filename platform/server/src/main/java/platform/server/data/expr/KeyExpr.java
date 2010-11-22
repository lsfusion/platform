package platform.server.data.expr;

import platform.server.caches.hash.HashContext;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    public void enumDepends(ExprEnumerator enumerator) {
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Type getType(KeyType keyType) {
        return keyType.getKeyType(this);
    }

    // возвращает Where без следствий
    public Where calculateWhere() {
        return Where.TRUE;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return translator.translate(this);
    }

    public KeyExpr translateOuter(MapTranslate translator) {
        return translator.translate(this);
    }

    public VariableExprSet calculateExprFollows() {
        return new VariableExprSet(this);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public int hashOuter(HashContext hashContext) {
        return hashContext.keys.hash(this);
    }

    public boolean twins(AbstractSourceJoin obj) {
        return false;
    }

    public void fillFollowSet(DataWhereSet fillSet) {
    }

    public long calculateComplexity() {
        return 1;
    }
}
