package platform.server.logics.property.actions.edit;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.interop.form.ServerResponse;
import platform.server.classes.ActionClass;
import platform.server.classes.DataClass;
import platform.server.classes.FileClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.CustomActionProperty;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.*;
import static platform.server.logics.ServerResourceBundle.getString;

public class DefaultChangeActionProperty<P extends PropertyInterface> extends CustomActionProperty {

    private final PropertyMapImplement<P, ClassPropertyInterface> implement;
    private final String editActionSID;
    private final Property filterProperty;

    private final Map<ClassPropertyInterface, P> mapInterfaces;

    public Map<ClassPropertyInterface, P> getMapInterfaces() {
        return mapInterfaces;
    }

    public DefaultChangeActionProperty(String sID, String caption, Property<P> property, List<P> listInterfaces, List<ValueClass> valueClasses, String editActionSID, Property filterProperty) {
        super(sID, caption, valueClasses.toArray(new ValueClass[valueClasses.size()]));

        mapInterfaces = getMapInterfaces(listInterfaces);
        this.implement = new PropertyMapImplement<P, ClassPropertyInterface>(property, reverse(mapInterfaces));
        this.editActionSID = editActionSID;
        this.filterProperty = filterProperty;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {

        Map<ClassPropertyInterface,DataObject> keys = context.getKeys();
        Map<ClassPropertyInterface,PropertyObjectInterfaceInstance> objectInstances = context.getObjectInstances();
        Modifier modifier = context.getModifier();
        final FormInstance<?> formInstance = context.getFormInstance();

        final PropertyValueImplement<P> propertyValues = implement.mapValues(keys);

        if (!(context.getSecurityPolicy().property.change.checkPermission(implement.property) &&
                (formInstance.entity.isActionOnChange(implement.property) || propertyValues.canBeChanged(modifier))))
            return;

        Type changeType = implement.property.getType();

        ObjectValue changeValue;
        if (changeType instanceof ActionClass) {
            changeValue = ActionClass.TRUE;
        } else {
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
        }

        if(changeValue != null) {
//            if(GROUP_CHANGE.equals(editActionSID))
//                implement.execute(getGroupChange(groupObject, modifier, keys, objectInstances, changeValue), context.getEnv(), objectInstances);
//            else // можно было бы в одну ветку сделать, но для оптимизации в том числе так
                implement.execute(keys, context.getEnv(), changeValue, objectInstances);
        }
    }

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
