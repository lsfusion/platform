package lsfusion.server.form.stat;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.FontInfo;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.BusinessLogics;

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
    public Object read(CalcPropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException {
        CalcPropertyObjectInstance propInstance = getInstance(reportPathProp, form);
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

    private static final String tablePrefix = "table";
    private String getReportPrefix(Integer groupId) {
        return groupId == null ? "" : tablePrefix + getFormEntity().getGroupObject(groupId).getSID() + "_";
    }

    @Override
    public String getReportPrefix() {
        return groupId != null ? getReportPrefix(groupId) : "";
    }
}
