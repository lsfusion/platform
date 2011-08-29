package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.classes.DataClass;
import platform.server.data.Time;
import platform.server.data.where.MapWhere;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public class TimeExpr extends StaticExpr<DataClass> {

    private final Time time;

    public TimeExpr(Time time) {
        super(time.getConcreteValueClass());
        this.time = time;
    }

    public BaseExpr translateOuter(MapTranslate translator) {
        return this;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }

    public boolean twins(TwinImmutableInterface obj) {
        return time.equals(((TimeExpr)obj).time);
    }

    public int hashOuter(HashContext hashContext) {
        return 6543 + time.hashCode();
    }

    public String getSource(CompileSource compile) {
        return time.getSource(compile);
    }
}
