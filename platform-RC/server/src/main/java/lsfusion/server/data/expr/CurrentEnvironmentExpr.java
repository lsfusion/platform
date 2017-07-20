package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.sets.AndClassSet;
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

    public String getSource(CompileSource compile) {
        return paramString;
    }
}
