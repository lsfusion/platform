package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.StaticClass;
import lsfusion.server.data.ParseValue;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

public class StaticValueExpr extends StaticExpr<StaticClass> implements ParseValue {

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

    public boolean calcTwins(TwinImmutableObject o) {
        return object.equals(((StaticValueExpr)o).object) && objectClass.equals(((StaticValueExpr)o).objectClass) && sID==((StaticValueExpr)o).sID;
    }

    public String getSource(CompileSource compile) {
        if (compile instanceof ToString) {
            return object + " - " + objectClass + " sID";
        }
        if (sID) {
            return objectClass.getString(object, compile.syntax);
        } else {
            Type type = objectClass.getType();
            String result;
            if (type.isSafeString(object)) {
                result = type.getString(object, compile.syntax);
            } else {
                result = compile.params.get(this);
            }
            if (!type.isSafeType(object))
                result = type.getCast(result, compile.syntax, compile.env);
            return result;
        }
    }

    @Override
    public ImSet<StaticValueExpr> getOuterStaticValues() {
        if(!objectClass.getType().isSafeString(object))
            return SetFact.singleton(this);

        return super.getOuterStaticValues();
    }


    public TypeObject getParseInterface() {
        assert !sID && !objectClass.getType().isSafeString(object);
        return new TypeObject(object, objectClass.getType());
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
