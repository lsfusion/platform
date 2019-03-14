package lsfusion.server.logics.form.stat.report;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.form.stat.FormDataInterface;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.sql.SQLException;

public interface FormReportInterface extends FormDataInterface {

    BusinessLogics getBL();

    Object read(PropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException;

    String getReportPrefix();

    FontInfo getUserFont(GroupObjectEntity entity);
    Integer getUserWidth(PropertyDrawEntity entity);
    ImOrderSet<PropertyDrawEntity> getUserOrder(GroupObjectEntity entity, ImOrderSet<PropertyDrawEntity> properties); // with user order
}
