package lsfusion.server.logics.form.stat.print;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.stat.StaticFormDataInterface;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.sql.SQLException;

public class StaticFormReportInterface extends StaticFormDataInterface implements FormReportInterface {

    public StaticFormReportInterface(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, ExecutionContext<?> context, ImSet<ContextFilterInstance> contextFilters) {
        super(form, mapObjects, context, contextFilters);
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
    public Object read(PropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException {
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
    public String getUserPattern(PropertyDrawEntity entity) {
        return null;
    }

    @Override
    public String getReportPrefix() {
        return "";
    }
}
