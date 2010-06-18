package platform.server.data.expr;

import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DoubleClass;
import platform.server.data.Time;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.data.query.JoinData;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public class TimeExpr extends StaticClassExpr {

    private final Time time;

    public TimeExpr(Time time) {
        this.time = time;
    }

    public ConcreteClass getStaticClass() {
        return DoubleClass.instance;
    }

    public VariableExprSet calculateExprFollows() {
        return new VariableExprSet();
    }

    public BaseExpr translate(MapTranslate translator) {
        return this;
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public Type getType(KeyType keyType) {
        return getStaticClass().getType();
    }

    public Where calculateWhere() {
        return Where.TRUE;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }

    public boolean twins(AbstractSourceJoin obj) {
        return time.equals(((TimeExpr)obj).time);
    }

    public int hashContext(HashContext hashContext) {
        return 6543;
    }

    public String getSource(CompileSource compile) {
        switch(time) {
            case HOUR:
                return compile.syntax.getHour();
            case EPOCH:
                return compile.syntax.getEpoch();
        }
        throw new RuntimeException("Unknown time");
    }

    public void enumerate(ContextEnumerator enumerator) {
    }
}
