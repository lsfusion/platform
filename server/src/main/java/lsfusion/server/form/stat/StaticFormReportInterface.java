package lsfusion.server.form.stat;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class StaticFormReportInterface extends StaticFormDataInterface implements FormReportInterface {

    public StaticFormReportInterface(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context) {
        super(form, mapObjects, context);
    }

    @Override
    public BusinessLogics getBL() {
        return context == null ? ThreadLocalContext.getBusinessLogics() : context.getBL(); // для getAllCustomReports надо
    }

    @Override
    public ImOrderSet<PropertyDrawEntity> getUserOrder(GroupObjectEntity entity, ImOrderSet<PropertyDrawEntity> properties) {
        return properties;
    }

    @Override
    public Object read(CalcPropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException {
        return reportPathProp.read(context.getEnv(), mapObjects);
    }

    @Override
    public FontInfo getUserFont(GroupObjectEntity entity) {
        return null;
    }

    @Override
    public Integer getUserWidth(PropertyDrawEntity entity) {
        return null;
    }

    @Override
    public String getReportPrefix() {
        return "";
    }
}
