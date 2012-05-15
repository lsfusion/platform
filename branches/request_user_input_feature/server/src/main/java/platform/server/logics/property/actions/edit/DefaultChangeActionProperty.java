package platform.server.logics.property.actions.edit;

import platform.interop.form.ServerResponse;
import platform.server.classes.DataClass;
import platform.server.classes.FileClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.*;
import static platform.server.logics.ServerResourceBundle.getString;

public class DefaultChangeActionProperty<P extends PropertyInterface> extends CustomActionProperty {

    private final CalcPropertyMapImplement<P, ClassPropertyInterface> implement;
    private final String editActionSID;
    private final CalcProperty filterProperty;

    public DefaultChangeActionProperty(String sID, String caption, CalcProperty<P> property, List<P> listInterfaces, List<ValueClass> valueClasses, String editActionSID, CalcProperty filterProperty) {
        super(sID, caption, valueClasses.toArray(new ValueClass[valueClasses.size()]));

        this.implement = new CalcPropertyMapImplement<P, ClassPropertyInterface>(property, reverse(getMapInterfaces(listInterfaces)));
        this.editActionSID = editActionSID;
        this.filterProperty = filterProperty;
    }

    @Override
    public void executeCustom(ExecutionContext context) throws SQLException {

        Map<ClassPropertyInterface,DataObject> keys = context.getKeys();
        Modifier modifier = context.getModifier();
        final FormInstance<?> formInstance = context.getFormInstance();

        final PropertyValueImplement<P> propertyValues = implement.mapValues(keys);

        if (!context.getSecurityPolicy().property.change.checkPermission(implement.property))
            return;

        Type changeType = implement.property.getType();

        if(!(formInstance.entity.isActionOnChange(implement.property) || propertyValues.canBeChanged(modifier)))
            return;

        ObjectValue changeValue;
        if (changeType instanceof DataClass) {
            Object oldValue = null;
            //не шлём значения для файлов, т.к. на клиенте они не нужны, но весят много
            if (!(changeType instanceof FileClass)) {
                oldValue = implement.read(context, keys);
            }
            changeValue = context.requestUserData((DataClass) changeType, oldValue);
        } else if (changeType instanceof ObjectType) {
            if (ServerResponse.EDIT_OBJECT.equals(editActionSID)) {
                context.requestUserObject(new ExecutionContext.RequestDialog() {
                    public DialogInstance createDialog() throws SQLException {
                        return formInstance.createObjectEditorDialog(propertyValues);
                    }
                });
                return;
            } else {
                final GroupObjectInstance groupObject = context.getGroupObjectInstance();
                changeValue = context.requestUserObject(new ExecutionContext.RequestDialog() {
                    public DialogInstance createDialog() throws SQLException {
                        return formInstance.createChangeEditorDialog(propertyValues, groupObject, filterProperty);
                    }
                });
            }
        } else
            throw new RuntimeException("not supported");

        if(changeValue != null) {
            implement.change(keys, context.getEnv(), changeValue);
        }
    }
//            if(GROUP_CHANGE.equals(editActionSID))
//                implement.execute(getGroupChange(groupObject, modifier, keys, objectInstances, changeValue), context.getEnv(), objectInstances);
//            else // можно было бы в одну ветку сделать, но для оптимизации в том числе так

/*    private PropertyChange<ClassPropertyInterface> getGroupChange(GroupObjectInstance groupObject, Modifier modifier, Map<ClassPropertyInterface, DataObject> keys, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> objectInstances, ObjectValue objectValue) {
        // "генерим" ключи для объектов, которые в группе объектов
        Map<ClassPropertyInterface, ObjectInstance> usedGroupObjects = filterValues(objectInstances, groupObject.objects);
        Map<ClassPropertyInterface, KeyExpr> mapKeys = KeyExpr.getMapKeys(usedGroupObjects.keySet());
        Map<ClassPropertyInterface, DataObject> restKeys = BaseUtils.filterNotKeys(keys, usedGroupObjects.keySet());
        Where changeWhere = Where.TRUE;
        Map<ObjectInstance, KeyExpr> groupKeys = new HashMap<ObjectInstance, KeyExpr>();
        for(Map.Entry<ClassPropertyInterface, KeyExpr> mapKey : mapKeys.entrySet()) {
            ObjectInstance usedGroupObject = usedGroupObjects.get(mapKey.getKey());
            KeyExpr groupKey = groupKeys.get(usedGroupObject);
            if(groupKey==null)
                groupKeys.put(usedGroupObject, mapKey.getValue());
            else
                changeWhere = changeWhere.and(mapKey.getValue().compare(groupKey, Compare.EQUALS));
        }

        changeWhere = changeWhere.and(groupObject.getWhere(groupKeys, modifier));
        return new PropertyChange<ClassPropertyInterface>(restKeys, mapKeys, objectValue.getExpr(), changeWhere);
    }*/
}
