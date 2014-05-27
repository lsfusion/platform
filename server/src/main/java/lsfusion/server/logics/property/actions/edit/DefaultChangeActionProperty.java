package lsfusion.server.logics.property.actions.edit;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.AsyncGetRemoteChangesClientAction;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.interop.form.ServerResponse;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import lsfusion.server.session.Modifier;

import java.io.IOException;
import java.sql.SQLException;

public class DefaultChangeActionProperty<P extends PropertyInterface> extends SystemExplicitActionProperty {

    private final CalcPropertyMapImplement<P, ClassPropertyInterface> implement;
    private final String editActionSID;
    private final CalcProperty filterProperty;

    public DefaultChangeActionProperty(String sID, String caption, CalcProperty<P> property, ImOrderSet<P> listInterfaces, ImList<ValueClass> valueClasses, String editActionSID, CalcProperty filterProperty) {
        super(sID, caption, valueClasses.toArray(new ValueClass[valueClasses.size()]));
        
        assert filterProperty==null || filterProperty.interfaces.size()==1;
        assert listInterfaces.size() == property.interfaces.size();

        this.implement = new CalcPropertyMapImplement<P, ClassPropertyInterface>(property, getMapInterfaces(listInterfaces).reverse());
        this.editActionSID = editActionSID;
        this.filterProperty = filterProperty;
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
        Type type = getImplementType();
        if (type instanceof DataClass) {
            return type;
        }
        return null;
    }

    public Type getImplementType() {
        return implement.property.getType();
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ImMap<ClassPropertyInterface,DataObject> keys = context.getDataKeys();
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
                        context.requestUserObject(
                                formInstance.createObjectEditorDialogRequest(propertyValues, context)
                        );
                        return;
                    }

                    changeValue = context.requestUserObject(
                            formInstance.createChangeEditorDialogRequest(propertyValues, context.getChangingPropertyToDraw(), filterProperty, context)
                    );

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
