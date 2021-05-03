package lsfusion.server.logics.form.interactive.action.change;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.time.IntervalClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DefaultChangeIntervalObjectAction extends SystemExplicitAction {

    private final ImOrderSet<ObjectEntity> objects;
    private final ObjectEntity intervalObjectEntity;
    private final String type;

    public DefaultChangeIntervalObjectAction(ImOrderSet<ObjectEntity> objects, ObjectEntity intervalObjectEntity, String type) {
        super(LocalizedString.create("CO", false), objects.get(0).baseClass, objects.get(1).baseClass);

        this.objects = objects;
        this.intervalObjectEntity = intervalObjectEntity;
        this.type = type;

        finalizeInit();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        final FormInstance formInstance = context.getFormFlowInstance();

        ObjectInstance objectFromInstance = formInstance.instanceFactory.getInstance(objects.get(0));
        ObjectInstance objectToInstance = formInstance.instanceFactory.getInstance(objects.get(1));

        ObjectValue changeValue = context.requestUserData((DataClass) intervalObjectEntity.baseClass.getBaseClass(),
                getOldValue(objectFromInstance, objectToInstance), true);

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

    private BigDecimal getOldValue(ObjectInstance from, ObjectInstance to) {
        Object valueFrom = from.getObjectValue().getValue();
        Object valueTo = to.getObjectValue().getValue();
        ZoneOffset offset = OffsetDateTime.now().getOffset();
        long secondsFrom;
        long secondsTo;
        if (type.equals("TIME")) {
            secondsFrom = ((LocalTime) valueFrom).atDate(LocalDate.now()).toEpochSecond(offset);
            secondsTo = ((LocalTime) valueTo).atDate(LocalDate.now()).toEpochSecond(offset);
        } else if (type.equals("DATE")) {
            secondsFrom = ((LocalDate) valueFrom).atStartOfDay().toEpochSecond(offset);
            secondsTo = ((LocalDate) valueTo).atStartOfDay().toEpochSecond(offset);
        } else {
            secondsFrom = ((LocalDateTime) valueFrom).toEpochSecond(offset);
            secondsTo = ((LocalDateTime) valueTo).toEpochSecond(offset);
        }

        return new BigDecimal(secondsFrom + "." + secondsTo);
    }

}
