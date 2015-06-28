package lsfusion.server.data;

import lsfusion.base.TwinImmutableObject;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.StaticNotNullExpr;
import lsfusion.server.data.query.CompileSource;

// эмулируем ключ равный значению, используется в нескольких не принципиальных эвристиках
public class StaticParamNotNullExpr extends StaticNotNullExpr {

    private final String name;
    public StaticParamNotNullExpr(AndClassSet paramClass, String name) {
        super(paramClass);
        this.name = name;
    }

    protected int hash(HashContext hash) {
        return System.identityHashCode(this);
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return "V(" + name + ")";

        throw new UnsupportedOperationException();
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return false;
    }

    @Override
    public int getStaticEqualClass() {
        return 2;
    }
}
