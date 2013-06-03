package lsfusion.server.logics.property.actions;

import lsfusion.interop.ClassViewType;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ChangeReadObjectActionProperty extends SystemExplicitActionProperty {

    private final CalcProperty filterProperty;

    public ChangeReadObjectActionProperty(CalcProperty filterProperty, ValueClass baseClass) {
        super("CO_" + filterProperty, baseClass);
        this.filterProperty = filterProperty;
    }

    @Override
    protected boolean isVolatile() {
        return true;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        final FormInstance<?> formInstance = context.getFormInstance();
        PropertyObjectInterfaceInstance singleObjectInstance = context.getSingleObjectInstance();

        if (singleObjectInstance instanceof ObjectInstance) {
            ObjectInstance objectInstance = (ObjectInstance) singleObjectInstance;
            if (objectInstance.groupTo.curClassView == ClassViewType.PANEL) { // в grid'е диалог не имеет смысла
                final ObjectValue oldValue = objectInstance.getObjectValue();
                ObjectValue changeValue;
                if (objectInstance instanceof CustomObjectInstance) {
                    final CustomObjectInstance customObjectInstance = (CustomObjectInstance) objectInstance;
                    changeValue = context.requestUserObject(new ExecutionContext.RequestDialog() {
                        public DialogInstance createDialog() throws SQLException {
                            return formInstance.createChangeObjectDialog(customObjectInstance.getBaseClass(), oldValue, customObjectInstance.groupTo, filterProperty);
                        }
                    });
                } else {
                    changeValue = context.requestUserData(((DataObjectInstance) objectInstance).getBaseClass(), oldValue.getValue());
                }
                if (changeValue != null) {
                    formInstance.changeObject(objectInstance, changeValue);
                }
            }
        }
    }
}
