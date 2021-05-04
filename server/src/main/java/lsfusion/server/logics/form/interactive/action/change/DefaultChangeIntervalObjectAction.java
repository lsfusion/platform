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
        super(LocalizedString.create("CO", false));

        this.type = type;
        this.objectFrom = objectFrom;
        this.objectTo = objectTo;

        finalizeInit();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        final FormInstance formInstance = context.getFormFlowInstance();

        ObjectValue changeValue = context.requestUserData(IntervalClass.getInstance(type).getBaseClass(), null, false);

        if (changeValue != null)
            changeObjects(formInstance, changeValue, true, false);
    }

    private void changeObjects(FormInstance formInstance, ObjectValue changeValue, boolean... positions) throws SQLException, SQLHandledException {
        for (boolean position : positions) {
            ObjectInstance objectInstance = formInstance.instanceFactory.getInstance(position ? objectFrom : objectTo);
            formInstance.changeObject(objectInstance, DataObject.getValue(getValue((BigDecimal) changeValue.getValue(), position), objectInstance.getCurrentClass()));
        }
    }

    private Object getValue(BigDecimal value, boolean from) {
        ZonedDateTime dateTime = IntervalClass.getLocalDateTime(value, from).atZone(ZoneId.systemDefault());
        return type.equals("TIME") ? dateTime.toLocalTime() : type.equals("DATE") ? dateTime.toLocalDate() : dateTime.toLocalDateTime();
    }
}
