package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class FilterPropertyAction extends SystemExplicitAction {
    private final String property;

    private final ClassPropertyInterface fromInterface;

    public FilterPropertyAction(String property, ValueClass... valueClasses) {
        super(valueClasses);
        this.property = property;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        this.fromInterface = orderInterfaces.get(0);
    }
    
    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        FormInstance formInstance = context.getFormInstance(true, true);
        PropertyDrawEntity propertyEntity = formInstance.entity.getPropertyDraw(property);
        if(propertyEntity != null) {
            GroupObjectEntity toDraw = propertyEntity.getToDraw(formInstance.entity);
            String groupObject = formInstance.entity.getSID() + "." + toDraw.getSID();
            String value = (String) context.getKeyObject(fromInterface);
            context.getBL().userEventsLM.filterPropertyAction.execute(context, new DataObject(groupObject), new DataObject(propertyEntity.getSID()),
                    ObjectValue.getValue(value, StringClass.instance));
        }
    }
}