package lsfusion.server.logics.form.interactive.action.change;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.AsyncGetRemoteChangesClientAction;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.action.UpdateEditValueClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.change.SetAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyValueImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;

public class DefaultChangeAction<P extends PropertyInterface> extends AbstractDefaultChangeAction<P> {

    private final String editActionSID;
    private final Property filterProperty;

    public DefaultChangeAction(LocalizedString caption, Property<P> property, ImOrderSet<P> listInterfaces, ImList<ValueClass> valueClasses, String editActionSID, Property filterProperty) {
        super(caption, property, listInterfaces, valueClasses.toArray(new ValueClass[valueClasses.size()]));

        assert editActionSID.equals(ServerResponse.EDIT_OBJECT) || property.canBeChanged();
        assert filterProperty==null || filterProperty.interfaces.size()==1;
        assert listInterfaces.size() == property.interfaces.size();

        this.editActionSID = editActionSID;
        this.filterProperty = filterProperty;
    }

    @Override // сам выполняет request поэтому на inRequest не смотрим
    public Type getSimpleRequestInputType(boolean optimistic, boolean inRequest) {
        Type type = getImplementType();
        if (type instanceof DataClass) {
            return type;
        }
        return null;
    }

    private Type getImplementType() {
        return implement.property.getType();
    }

    protected ObjectValue requestValue(ExecutionContext<ClassPropertyInterface> context, ImMap<ClassPropertyInterface, DataObject> keys, PropertyValueImplement<P> propertyValues) throws SQLException, SQLHandledException {
        final FormInstance formInstance = context.getFormFlowInstance();
        Type changeType = getImplementType();

        ObjectValue changeValue = null;
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
            } else {
                changeValue = context.requestUserObject(
                        formInstance.createChangeEditorDialogRequest(propertyValues, context.getChangingPropertyToDraw(), filterProperty, context.stack)
                );

                if (filterProperty != null && changeValue != null) {
                    Object updatedValue = filterProperty.read(
                            context.getSession().sql, MapFact.singleton(filterProperty.interfaces.single(), changeValue), context.getModifier(), context.getQueryEnv()
                    );

                    try {
                        context.delayUserInteraction(new UpdateEditValueClientAction(BaseUtils.serializeObject(updatedValue)));
                    } catch (IOException e) {
                        Throwables.propagate(e);
                    }
                    context.delayUserInteraction(new AsyncGetRemoteChangesClientAction());
                }
            }
        } else {
            throw new RuntimeException("not supported");
        }
        return changeValue;
    }
}
