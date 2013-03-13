package platform.server.data.expr;

import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.classes.StaticClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

public class StaticValueExpr extends StaticExpr<StaticClass> {

    private final Object object;
    private boolean sID;

    public StaticValueExpr(Object value, StaticClass customClass, boolean sID) {
        super(customClass);

        this.object = value;
        this.sID = sID;
    }

    public StaticValueExpr(Object value, StaticClass customClass) {
        this(value, customClass, false);
    }

    public StaticValueExpr(Object value, ConcreteCustomClass customClass, boolean sID) {
        this(value, (StaticClass)customClass, sID);
    }

    protected StaticValueExpr translate(MapTranslate translator) {
        return this;
    }

    public StaticValueExpr translateQuery(QueryTranslator translator) {
        return this;
    }

    protected int hash(HashContext hashContext) {
        return object.hashCode() * 31 + objectClass.hashCode() + 6;
    }

    public boolean twins(TwinImmutableObject o) {
        return object.equals(((StaticValueExpr)o).object) && objectClass.equals(((StaticValueExpr)o).objectClass) && sID==((StaticValueExpr)o).sID;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return object + " - " + objectClass + " sID";
        if(sID)
            return ((ConcreteCustomClass)objectClass).getString(object,compile.syntax);
        else {
            Type type = objectClass.getType();
            String result = type.getString(object, compile.syntax);
            if(!type.isSafeType(object))
                result = type.getCast(result, compile.syntax, true); // cast часто rtrim делает и глотает проблемы
            return result;
        }
    }

    @Override
    public ObjectValue getObjectValue() {
        if(sID)
            return ((ConcreteCustomClass)objectClass).getDataObject((String) object);
        else
            return new DataObject(object, objectClass);
    }

    @Override
    public boolean compatibleEquals(BaseExpr expr) {
        return super.compatibleEquals(expr) || !sID && expr instanceof StaticValueExpr && !((StaticValueExpr)expr).sID &&
                objectClass instanceof DataClass && ((StaticValueExpr) expr).objectClass instanceof DataClass &&
                ((DataClass) objectClass).compatibleEquals(object, (DataClass) ((StaticValueExpr) expr).objectClass, ((StaticValueExpr) expr).object);
    }
}
