package platform.server.data.expr;

import platform.server.caches.hash.HashContext;
import platform.server.classes.ConcreteClass;
import platform.server.data.query.CompileSource;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

// значение которое не транслируется, смысл в том что там где делается mapping(getExpr\parse\compile) если создается новый ValueExpr то он не должен транслироваться, сравниваться и т.п. 
public class SystemValueExpr extends AbstractValueExpr {

    public SystemValueExpr(Object object, ConcreteClass objectClass) {
        super(object, objectClass);

        assert objectClass.getType().isSafeString(object);
    }

    public SystemValueExpr translateOuter(MapTranslate translator) { // ради этого во многом и делается
        return this;
    }

    public SystemValueExpr translateQuery(QueryTranslator translator) {
        return this;
    }

    public int hashOuter(HashContext hashContext) {
        return object.hashCode() * 31 + objectClass.hashCode() + 5;
    }

    public String getSource(CompileSource compile) {
        return objectClass.getType().getString(object, compile.syntax);
    }

    public void enumerate(ContextEnumerator enumerator) {
    }
}
