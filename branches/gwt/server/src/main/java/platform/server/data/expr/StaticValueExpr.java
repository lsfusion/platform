package platform.server.data.expr;

import platform.server.caches.hash.HashContext;
import platform.server.classes.StaticClass;
import platform.server.classes.StaticCustomClass;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

public class StaticValueExpr extends AbstractValueExpr {

    public StaticValueExpr(Object value, StaticClass customClass) {
        super(value, customClass);
    }

    public StaticValueExpr translateOuter(MapTranslate translator) {
        return this;
    }

    public StaticValueExpr translateQuery(QueryTranslator translator) {
        return this;
    }

    public int hashOuter(HashContext hashContext) {
        return object.hashCode() * 31 + objectClass.hashCode() + 6;
    }

    public String getSource(CompileSource compile) {
        return ((StaticClass)objectClass).getString(object,compile.syntax);
    }
}
