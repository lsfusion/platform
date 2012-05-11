package platform.server.logics.property.actions;

import platform.interop.ClassViewType;
import platform.server.classes.BaseClass;
import platform.server.form.instance.*;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.Property;

import java.sql.SQLException;

public class DialogChangeObjectActionProperty extends CustomActionProperty {

    private final Property filterProperty;

    public DialogChangeObjectActionProperty(Property filterProperty, BaseClass baseClass) {
        super("CO_"+filterProperty, baseClass);
        this.filterProperty = filterProperty;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {

        final FormInstance<?> formInstance = context.getFormInstance();
        PropertyObjectInterfaceInstance singleObjectInstance = context.getSingleObjectInstance();

        if(singleObjectInstance instanceof CustomObjectInstance) {
            final CustomObjectInstance objectInstance = (CustomObjectInstance) singleObjectInstance;
            if(objectInstance.groupTo.curClassView == ClassViewType.PANEL) { // в grid'е диалог не имеет смысла
                ObjectValue dialogValue = context.requestUserObject(new ExecutionContext.RequestDialog() {
                    public DialogInstance createDialog() throws SQLException {
                        return formInstance.createChangeObjectDialog(objectInstance.getBaseClass(), objectInstance.getObjectValue().getValue(), objectInstance.groupTo, filterProperty);
                    }
                });
                if(dialogValue!=null)
                    formInstance.changeObject(objectInstance, dialogValue, context.getActions());
            }
        }
    }
}
