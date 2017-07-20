package lsfusion.server.logics.property;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.ObjectValue;

import java.sql.SQLException;

public interface DialogRequest {

    FormInstance createDialog() throws SQLException, SQLHandledException;

    ObjectValue getValue();
}
