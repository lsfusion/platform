package lsfusion.server.logics.form.interactive.action.change;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.AsyncGetRemoteChangesClientAction;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.change.SetAction;
import lsfusion.server.logics.classes.DataClass;
import lsfusion.server.logics.classes.FileClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.session.change.modifier.Modifier;

import java.io.IOException;
import java.sql.SQLException;

public class DefaultChangeActionProperty<P extends PropertyInterface> extends SystemExplicitAction {

    private final PropertyMapImplement<P, ClassPropertyInterface> implement;
    private final String editActionSID;
    private final Property filterProperty;

    public DefaultChangeActionProperty(LocalizedString caption, Property<P> property, ImOrderSet<P> listInterfaces, ImList<ValueClass> valueClasses, String editActionSID, Property filterProperty) {
        super(caption, valueClasses.toArray(new ValueClass[valueClasses.size()]));

        assert editActionSID.equals(ServerResponse.EDIT_OBJECT) || property.canBeChanged();
        assert filterProperty==null || filterProperty.interfaces.size()==1;
        assert listInterfaces.size() == property.interfaces.size();

        this.implement = new PropertyMapImplement<>(property, getMapInterfaces(listInterfaces).reverse());
        this.editActionSID = editActionSID;
        this.filterProperty = filterProperty;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if(SetAction.hasFlow(implement, type))
            return true;
        return super.hasFlow(type);
    }

    @Override // сам выполняет request поэтому на inRequest не смотрим
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
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
    protected boolean isSync() {
        return true;
    }

    @Override
    protected boolean allowNulls() {
        return false;
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ImMap<ClassPropertyInterface,DataObject> keys = context.getDataKeys();
        Modifier modifier = context.getModifier();
        final FormInstance formInstance = context.getFormFlowInstance();

        final PropertyValueImplement<P> propertyValues = implement.mapValues(keys);

        Type changeType = getImplementType();
        
        if (propertyValues.canBeChanged(modifier)) {
            ObjectValue changeValue;
            if (changeType instanceof DataClass) {
                Object oldValue = null;
                // optimization. we don't use files on client side (see also ScriptingLogicsModule.addScriptedInputAProp())
                if (!(changeType instanceof FileClass)) {
                    oldValue = implement.read(context, keys);
                }
                changeValue = context.requestUserData((DataClass) changeType, oldValue);
            } else if (changeType instanceof ObjectType) {
                if (ServerResponse.EDIT_OBJECT.equals(editActionSID)) {
                    ObjectValue currentObject = propertyValues.readClasses(context);
                    if(currentObject instanceof DataObject) // force notnull для edit'а по сути
                        context.getBL().LM.getFormEdit().execute(context, currentObject);
//                        context.requestUserObject(
//                                formInstance.createObjectEditorDialogRequest(propertyValues, context.stack)
//                        );
                    return;
                }

                changeValue = context.requestUserObject(
                        formInstance.createChangeEditorDialogRequest(propertyValues, context.getChangingPropertyToDraw(), filterProperty, context.stack)
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

    

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(implement.property);
    }
}
