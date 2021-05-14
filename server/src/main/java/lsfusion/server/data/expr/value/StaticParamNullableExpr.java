package lsfusion.server.data.expr.value;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;

// эмулируем ключ равный значению, используется в нескольких не принципиальных эвристиках
public class StaticParamNullableExpr extends StaticNullableExpr {

    private final String name;
    public StaticParamNullableExpr(ValueClass paramClass, String name) {
        super(paramClass.getUpSet());
        this.name = name;
    }

    public int hash(HashContext hash) {
        return System.identityHashCode(this);
    }

    public String getSource(CompileSource compile, boolean needValue) {
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
