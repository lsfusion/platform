package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.data.Time;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

public class TimeExpr extends StaticExpr<DataClass> {

    private final Time time;

    public TimeExpr(Time time) {
        super(time.getConcreteValueClass());
        this.time = time;
    }

    protected BaseExpr translate(MapTranslate translator) {
        return this;
    }

    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }

    public boolean twins(TwinImmutableObject obj) {
        return time.equals(((TimeExpr)obj).time);
    }

    protected int hash(HashContext hashContext) {
        return 6543 + time.hashCode();
    }

    public String getSource(CompileSource compile) {
        return time.getSource(compile);
    }
}
