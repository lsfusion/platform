package lsfusion.server.logics.form.stat.print;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.stat.InteractiveFormDataInterface;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.sql.SQLException;

public class InteractiveFormReportInterface extends InteractiveFormDataInterface implements FormReportInterface {

    public InteractiveFormReportInterface(FormInstance form, Integer groupId, FormUserPreferences preferences) {
        super(form, groupId, preferences);
    }
    
    @Deprecated
    public FormInstance getForm() {
        return form;
    }

    @Override
    public BusinessLogics getBL() {
        return form.BL;
    }

    @Override
    public Object read(PropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException {
        PropertyObjectInstance propInstance = getInstance(reportPathProp, form);
        return propInstance.read(form);
    }

    @Override
    public ImOrderSet<PropertyDrawEntity> getUserOrder(GroupObjectEntity entity, ImOrderSet<PropertyDrawEntity> properties) {
        return form.getOrderedVisibleProperties(getInstance(entity, form), properties, preferences);
    }

    @Override
    public FontInfo getUserFont(GroupObjectEntity entity) {
        return form.getUserFont(getInstance(entity, form), preferences);
    }

    @Override
    public Integer getUserWidth(PropertyDrawEntity entity) {
        return form.getUserWidth(getInstance(entity, form), preferences);
    }

    @Override
    public String getUserPattern(PropertyDrawEntity entity) {
        return form.getUserPattern(getInstance(entity, form), preferences);
    }

    private static final String tablePrefix = "table";
    private String getReportPrefix(Integer groupId) {
        return groupId == null ? "" : tablePrefix + getFormEntity().getGroupObject(groupId).getSID() + "_";
    }

    @Override
    public String getReportPrefix() {
        return groupId != null ? getReportPrefix(groupId) : "";
    }
}
