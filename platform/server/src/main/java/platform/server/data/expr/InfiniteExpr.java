package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

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

    @Override
    public Expr translateQuery(QueryTranslator translator) {
        return this;
    }

    protected int hash(HashContext hashContext) {
        return objectClass.hashCode() + 17;
    }

    public String getSource(CompileSource compile) {
        return objectClass.getString(objectClass.getInfiniteValue(), compile.syntax);
    }

    public boolean twins(TwinImmutableObject o) {
        return objectClass.equals(((InfiniteExpr)o).objectClass);
    }
}
