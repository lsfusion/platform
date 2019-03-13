package lsfusion.server.data.expr;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.hash.HashContext;
import lsfusion.server.logics.classes.sets.AndClassSet;
import lsfusion.server.data.query.CompileSource;

public class CurrentEnvironmentExpr extends StaticNullableExpr {

    private final String paramString;

    public CurrentEnvironmentExpr(String paramString, AndClassSet paramClass) {
        super(paramClass);
        this.paramString = paramString;
    }
    public boolean calcTwins(TwinImmutableObject obj) {
        return paramString.equals(((CurrentEnvironmentExpr) obj).paramString);
    }

    protected int hash(HashContext hashContext) {
        return paramString.hashCode();
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return paramString;
    }
}
