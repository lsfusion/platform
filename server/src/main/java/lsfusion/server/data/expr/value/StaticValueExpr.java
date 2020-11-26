package lsfusion.server.data.expr.value;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.AbstractSourceJoin;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.StaticClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class StaticValueExpr extends AbstractValueExpr<StaticClass> {

    private final Object object;
    private boolean sID;

    public static void checkLocalizedString(Object value, StaticClass staticClass) {
        assert staticClass instanceof ConcreteCustomClass || staticClass instanceof StringClass || staticClass.getType().read(value).equals(value); // чтобы читалось то что писалось
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

    public int hash(HashContext hashContext) {
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

    public String getSource(CompileSource compile, boolean needValue) {
        if (compile instanceof AbstractSourceJoin.ToString) {
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


    public TypeObject getParseInterface(QueryEnvironment env, EnsureTypeEnvironment typeEnv) {
        assert isParameterized();

        return new TypeObject(getLocalizedObject(object, env), objectClass.getType());
    }

    @Override
    public boolean isAlwaysSafeString() {
        assert isParameterized();
        return super.isAlwaysSafeString();
    }

    @Override
    public Object getObject() {
        return object;
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

    @Override
    public String toString() {
        return object + " - " + objectClass + " : " + sID;
    }

    @Override
    public boolean isAlwaysPositiveOrNull() {
        if(objectClass instanceof IntegralClass) 
            return ((IntegralClass) objectClass).isPositive((Number) object);
        return super.isAlwaysPositiveOrNull();
    }
}
