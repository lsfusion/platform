package lsfusion.server.data.expr;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.type.Time;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.logics.classes.data.DataClass;

public class TimeExpr extends StaticExpr<DataClass> {

    private final Time time;

    public TimeExpr(Time time) {
        super(time.getConcreteValueClass());
        this.time = time;
    }

    protected BaseExpr translate(MapTranslate translator) {
        return this;
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return time.equals(((TimeExpr)obj).time);
    }

    public int hash(HashContext hashContext) {
        return 6543 + time.hashCode();
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return time.getSource(compile);
    }
}
