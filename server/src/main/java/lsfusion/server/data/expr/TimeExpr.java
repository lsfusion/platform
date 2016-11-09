package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.Time;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;

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

    public boolean calcTwins(TwinImmutableObject obj) {
        return time.equals(((TimeExpr)obj).time);
    }

    protected int hash(HashContext hashContext) {
        return 6543 + time.hashCode();
    }

    public String getSource(CompileSource compile) {
        return time.getSource(compile);
    }
}
