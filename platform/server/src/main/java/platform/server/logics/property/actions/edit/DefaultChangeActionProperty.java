package platform.server.logics.property.actions.edit;

import com.google.common.base.Throwables;
import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.interop.action.AsyncGetRemoteChangesClientAction;
import platform.interop.action.UpdateEditValueClientAction;
import platform.interop.action.EditNotPerformedClientAction;
import platform.interop.form.ServerResponse;
import platform.server.classes.DataClass;
import platform.server.classes.FileClass;
import platform.server.classes.ValueClass;
import platform.server.data.type.ObjectType;
import platform.server.data.type.Type;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.SystemActionProperty;
import platform.server.session.Modifier;

import java.io.IOException;
import java.sql.SQLException;

public class DefaultChangeActionProperty<P extends PropertyInterface> extends SystemActionProperty {

    private final CalcPropertyMapImplement<P, ClassPropertyInterface> implement;
    private final String editActionSID;
    private final CalcProperty filterProperty;

    public DefaultChangeActionProperty(String sID, String caption, CalcProperty<P> property, ImOrderSet<P> listInterfaces, ImList<ValueClass> valueClasses, String editActionSID, CalcProperty filterProperty) {
        super(sID, caption, valueClasses.toArray(new ValueClass[valueClasses.size()]));
        
        assert filterProperty==null || filterProperty.interfaces.size()==1;

        this.implement = new CalcPropertyMapImplement<P, ClassPropertyInterface>(property, getMapInterfaces(listInterfaces).reverse());
        this.editActionSID = editActionSID;
        this.filterProperty = filterProperty;
    }

    @Override
    public Type getSimpleRequestInputType() {
        Type type = getImplementType();
        if (type instanceof DataClass) {
            return type;
        }
        return null;
    }

    private Type getImplementType() {
        return implement.property.getType();
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        ImMap<ClassPropertyInterface,DataObject> keys = context.getKeys();
        Modifier modifier = context.getModifier();
        final FormInstance<?> formInstance = context.getFormInstance();

        final CalcPropertyValueImplement<P> propertyValues = implement.mapValues(keys);

        if (context.getSecurityPolicy().property.change.checkPermission(implement.property)) {
            Type changeType = getImplementType();

            if (propertyValues.canBeChanged(modifier)) {
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
                    }

                    changeValue = context.requestUserObject(new ExecutionContext.RequestDialog() {
                        public DialogInstance createDialog() throws SQLException {
                            return formInstance.createChangeEditorDialog(propertyValues, context.getGroupObjectInstance(), filterProperty);
                        }
                    });

                    if(filterProperty!=null && changeValue!=null) {
                        Object updatedValue = filterProperty.read(
                                context.getSession().sql, MapFact.singleton(filterProperty.interfaces.single(), changeValue), modifier, context.getQueryEnv()
                        );

                        try {
                            context.delayUserInteraction(new UpdateEditValueClientAction(BaseUtils.serializeObject(updatedValue)));
                        } catch (IOException e) {
                            Throwables.propagate(e);
                        }
                        context.delayUserInteraction(new AsyncGetRemoteChangesClientAction());
                    }
                } else {
                    throw new RuntimeException("not supported");
                }

                if (changeValue != null) {
                    implement.change(keys, context.getEnv(), changeValue);
                }

                return;
            }
        }

        context.delayUserInteraction(EditNotPerformedClientAction.instance);
    }
//            if(GROUP_CHANGE.equals(editActionSID))
//                implement.execute(getGroupChange(groupObject, modifier, keys, objectInstances, changeValue), context.getEnv(), objectInstances);
//            else // можно было бы в одну ветку сделать, но для оптимизации в том числе так

/*    private PropertyChange<ClassPropertyInterface> getGroupChange(GroupObjectInstance groupObject, Modifier modifier, Map<ClassPropertyInterface, DataObject> keys, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> objectInstances, ObjectValue objectValue) {
        // "генерим" ключи для объектов, которые в группе объектов
        Map<ClassPropertyInterface, ObjectInstance> usedGroupObjects = filterValues(objectInstances, groupObject.objects);
        Map<ClassPropertyInterface, KeyExpr> mapKeys = KeyExpr.getMapKeys(usedGroupObjects.keySet());
        Map<ClassPropertyInterface, DataObject> restKeys = BaseUtils.remove(keys, usedGroupObjects.keySet());
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
