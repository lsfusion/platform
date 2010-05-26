package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.classes.ActionClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.Field;
import platform.server.data.PropertyField;
import platform.server.data.KeyField;
import platform.server.data.type.Type;
import platform.server.logics.ObjectValue;
import platform.server.logics.DataObject;
import platform.server.view.form.client.RemoteFormView;
import platform.server.view.form.PropertyObjectImplement;
import platform.interop.action.ClientAction;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

public abstract class UserProperty extends Property<ClassPropertyInterface> {

    public static List<ClassPropertyInterface> getInterfaces(ValueClass[] classes) {
        List<ClassPropertyInterface> interfaces = new ArrayList<ClassPropertyInterface>();
        for(ValueClass interfaceClass : classes)
            interfaces.add(new ClassPropertyInterface(interfaces.size(),interfaceClass));
        return interfaces;
    }

    protected UserProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, getInterfaces(classes));
    }

    protected Map<ClassPropertyInterface, ValueClass> getMapClasses() {
        Map<ClassPropertyInterface, ValueClass> result = new HashMap<ClassPropertyInterface, ValueClass>();
        for(ClassPropertyInterface propertyInterface : interfaces)
            result.put(propertyInterface,propertyInterface.interfaceClass);
        return result;
    }

    @Override
    public DataChanges getDataChanges(PropertyChange<ClassPropertyInterface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        change = change.and(ClassProperty.getIsClassWhere(change.mapKeys, modifier, null).and(modifier.getSession().getIsClassWhere(change.expr, getValueClass(), changedWhere).or(change.expr.getWhere().not())));
        if(changedWhere !=null) changedWhere.add(change.where); // помечаем что можем обработать тока подходящие по интерфейсу классы
        // изменяет себя, если классы совпадают
        return new DataChanges(this, change);
    }

    public Type getType() {
        return getValueClass().getType();
    }

    protected abstract ValueClass getValueClass();

    protected ClassWhere<Field> getClassWhere(PropertyField storedField) {
        Map<Field, AndClassSet> result = new HashMap<Field, AndClassSet>();
        for(Map.Entry<ClassPropertyInterface, KeyField> mapKey : mapTable.mapKeys.entrySet())
            result.put(mapKey.getValue(), mapKey.getKey().interfaceClass.getUpSet());
        result.put(storedField, getValueClass().getUpSet());
        return new ClassWhere<Field>(result);
    }

    protected boolean usePreviousStored() {
        return false;
    }

    public abstract void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteFormView executeForm, PropertyObjectImplement<?> propertyImplement) throws SQLException;
}
