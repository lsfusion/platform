package lsfusion.server.logics.property;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.ObjectValue;

import java.sql.SQLException;

public abstract class DialogRequestAdapter implements DialogRequest {
    protected ObjectEntity dialogObject;
    private ObjectInstance dialogObjectInstance;

    public ObjectValue getValue() {
        assert dialogObjectInstance != null;
        return dialogObjectInstance.getObjectValue();
    }

    public final FormInstance createDialog() throws SQLException, SQLHandledException {
        FormInstance result = doCreateDialog();
        if (result != null) {
            dialogObjectInstance = result.instanceFactory.getInstance(dialogObject);
        }
        return result;
    }

    protected abstract FormInstance doCreateDialog() throws SQLException, SQLHandledException;
}
