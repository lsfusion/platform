package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.CustomObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.DataObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class DefaultChangeObjectAction extends SystemExplicitAction {

    private final ObjectEntity object;
    
    public DefaultChangeObjectAction(ValueClass baseClass, ObjectEntity object) {
        super(LocalizedString.create("CO", false), baseClass);
        this.object = object;
        
        finalizeInit();
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        final FormInstance formInstance = context.getFormFlowInstance();
        ObjectInstance objectInstance = formInstance.instanceFactory.getInstance(object);
        if (objectInstance.groupTo.classView.isPanel()) { // в grid'е диалог не имеет смысла
            final ObjectValue oldValue = objectInstance.getObjectValue();
            ObjectValue changeValue;
            if (objectInstance instanceof CustomObjectInstance) {
                final CustomObjectInstance customObjectInstance = (CustomObjectInstance) objectInstance;
                changeValue = context.inputUserObject(
                        formInstance.createChangeObjectDialogRequest(customObjectInstance.getBaseClass(), oldValue, customObjectInstance.groupTo, context.stack)
                );
            } else {
                changeValue = context.requestUserData(((DataObjectInstance) objectInstance).getBaseClass(), oldValue.getValue(), true);
            }
            if (changeValue != null) {
                formInstance.changeObject(objectInstance, changeValue);
            }
        }
    }
}
