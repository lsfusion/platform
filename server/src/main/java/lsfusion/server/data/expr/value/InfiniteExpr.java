package lsfusion.server.data.expr.value;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.logics.classes.data.DataClass;

public class InfiniteExpr extends StaticExpr<DataClass> {

    public InfiniteExpr(DataClass objectClass) {
        super(objectClass);
    }

    @Override
    public String toString() {
        return "Inf " + objectClass;
    }

    protected BaseExpr translate(MapTranslate translator) {
        return this;
    }

    public int hash(HashContext hashContext) {
        return objectClass.hashCode() + 17;
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return objectClass.getString(objectClass.getInfiniteValue(false), compile.syntax);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return objectClass.equals(((InfiniteExpr)o).objectClass);
    }

    @Override
    public int getStaticEqualClass() {
        return 1000;
    }
}
