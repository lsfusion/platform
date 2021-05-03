package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.time.IntervalClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DefaultChangeIntervalObjectAction extends SystemExplicitAction {

    private final ObjectEntity objectFrom;
    private final ObjectEntity objectTo;
    private final String type;

    public DefaultChangeIntervalObjectAction(ObjectEntity objectFrom, ObjectEntity objectTo, String type) {
        super(LocalizedString.create("CO", false), objectFrom.baseClass, objectTo.baseClass);

        this.type = type;
        this.objectFrom = objectFrom;
        this.objectTo = objectTo;

        finalizeInit();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        final FormInstance formInstance = context.getFormFlowInstance();

        ObjectInstance objectFromInstance = formInstance.instanceFactory.getInstance(objectFrom);
        ObjectInstance objectToInstance = formInstance.instanceFactory.getInstance(objectTo);

        ObjectValue changeValue = context.requestUserData(IntervalClass.getInstance(type).getBaseClass(), null, false);

        if (changeValue != null) {
            BigDecimal newValue = (BigDecimal) changeValue.getValue();

            formInstance.changeObject(objectFromInstance, DataObject.getValue(getValue(newValue, true), objectFromInstance.getCurrentClass()));
            formInstance.changeObject(objectToInstance, DataObject.getValue(getValue(newValue, false), objectToInstance.getCurrentClass()));
        }
    }

    private Object getValue(BigDecimal value, boolean from) {
        ZonedDateTime dateTime = IntervalClass.getLocalDateTime(value, from).atZone(ZoneId.systemDefault());
        return type.equals("TIME") ? dateTime.toLocalTime() : type.equals("DATE") ? dateTime.toLocalDate() : dateTime.toLocalDateTime();
    }
}
