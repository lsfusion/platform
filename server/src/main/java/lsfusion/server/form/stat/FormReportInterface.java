package lsfusion.server.form.stat;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.FontInfo;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.instance.FormDataInterface;
import lsfusion.server.logics.BusinessLogics;

import java.sql.SQLException;

public interface FormReportInterface extends FormDataInterface {

    BusinessLogics getBL();

    Object read(CalcPropertyObjectEntity reportPathProp) throws SQLException, SQLHandledException;

    String getReportPrefix();

    FontInfo getUserFont(GroupObjectEntity entity);
    Integer getUserWidth(PropertyDrawEntity entity);
    ImOrderSet<PropertyDrawEntity> getUserOrder(GroupObjectEntity entity, ImOrderSet<PropertyDrawEntity> properties); // with user order
}
