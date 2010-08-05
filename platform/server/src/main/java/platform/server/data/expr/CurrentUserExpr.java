package platform.server.data.expr;

import platform.server.caches.hash.HashContext;
import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

public class CurrentUserExpr extends VariableClassExpr {

    private final ValueClass userClass;

    public CurrentUserExpr(ValueClass userClass) {
        this.userClass = userClass;
    }

    protected VariableExprSet calculateExprFollows() {
        return new VariableExprSet();
    }

    public void fillFollowSet(DataWhereSet fillSet) {
    }

    public CurrentUserExpr translateOuter(MapTranslate translator) {
        return this;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Type getType(KeyType keyType) {
        return userClass.getType();
    }

    public Where calculateWhere() {
        return Where.TRUE;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }

    public boolean twins(AbstractSourceJoin obj) {
        return true;
    }

    public int hashOuter(HashContext hashContext) {
        return 7895;
    }

    public String getSource(CompileSource compile) {
        return SQLSession.userParam;
    }

    public void enumerate(ContextEnumerator enumerator) {
    }
}
