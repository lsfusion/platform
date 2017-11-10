package lsfusion.server.data.expr;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.StaticClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;

public class StaticValueExpr extends AbstractValueExpr<StaticClass> {

    private final Object object;
    private boolean sID;

    public static void checkLocalizedString(Object value, StaticClass staticClass) {
        assert !(staticClass instanceof StringClass) || value instanceof LocalizedString;
    }

    public StaticValueExpr(Object value, StaticClass customClass, boolean sID) {
        super(customClass);

        this.object = value;
        this.sID = sID;
        
        checkLocalizedString(object, customClass);
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

    protected int hash(HashContext hashContext) {
        return object.hashCode() * 31 + objectClass.hashCode() + 6;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return object.equals(((StaticValueExpr)o).object) && objectClass.equals(((StaticValueExpr)o).objectClass) && sID==((StaticValueExpr)o).sID;
    }
    
    public boolean isZero() {
        return objectClass.isZero(object);
    }

    private Object getAdjustObject(Object object) {
        Object adjObject = object;
        if(sID)
            adjObject = ((ConcreteCustomClass) objectClass).getObjectID((String) adjObject);
        return adjObject;
    }

    public String getSource(CompileSource compile) {
        if (compile instanceof ToString) {
            return object + " - " + objectClass + " " + sID;
        }
        Type type = objectClass.getType();
        String result;
        if (isParameterized())
            result = compile.params.get(this);
        else
            result = type.getString(getLocalizedObject(getAdjustObject(object)), compile.syntax);
        if (!type.isSafeType())
            result = type.getCast(result, compile.syntax, compile.env);
        return result;
    }

    private boolean isParameterized() {
        if(sID) // код объекта всегда можно inline'ть
            return false;

        Object adjObject = object;
        if(objectClass instanceof StringClass) { // если нужна локализация, придется все равно закидывать в параметры            
            LocalizedString locObject = (LocalizedString) adjObject;
            if (locObject.needToBeLocalized()) // если нужна локализация, придется все равно закидывать в параметры
                return true;
            adjObject = locObject.getSourceString();
        }

        if(objectClass.getType().isSafeString(adjObject)) // если значение можно inline'ть - inline'м
            return false;

        return true;
    }

    @Override
    public ImSet<StaticValueExpr> getOuterStaticValues() {
        if(isParameterized())
            return SetFact.singleton(this);

        return super.getOuterStaticValues();
    }


    public TypeObject getParseInterface(QueryEnvironment env) {
        assert isParameterized();

        return new TypeObject(getLocalizedObject(object, env), objectClass.getType());
    }

    @Override
    public boolean isAlwaysSafeString() {
        assert isParameterized();
        return super.isAlwaysSafeString();
    }

    private Object getLocalizedObject(Object object) {
        Object adjObject = object;
        if(objectClass instanceof StringClass) {
            LocalizedString locObject = (LocalizedString) adjObject;
            assert !locObject.needToBeLocalized();
            adjObject = locObject.getSourceString();
        }
        return adjObject;
    }

    private Object getLocalizedObject(Object object, QueryEnvironment env) {
        Object adjObject = object;
        if(objectClass instanceof StringClass) {
            LocalizedString locObject = (LocalizedString) adjObject;
            if(locObject.needToBeLocalized())
                adjObject = ThreadLocalContext.localize(locObject, env.getLocale());
            else
                adjObject = locObject.getSourceString();
        }
        return adjObject;
    }

    @Override
    public ObjectValue getObjectValue(QueryEnvironment env) {
        return new DataObject(getLocalizedObject(getAdjustObject(object), env), objectClass);
    }

    @Override
    public int getStaticEqualClass() {
        return 1;
    }
}
