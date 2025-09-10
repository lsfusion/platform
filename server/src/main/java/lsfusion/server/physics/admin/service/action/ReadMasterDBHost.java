package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class ReadMasterDBHost extends InternalAction {
    public ReadMasterDBHost(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            Long dbServer = (Long) findProperty("dbMaster[]").read(context.getSession());
            DataObject dbServerClass = new DataObject(dbServer, (ConcreteCustomClass) findClass("DBMaster"));

            findProperty("host[DBServer]").change(context.getDbManager().getAdapter().getMaster().host, context, dbServerClass);
            findProperty("name[DBServer]").change(LocalizedString.create("{service.scaling.master}").toString(),  context, dbServerClass);
            context.apply();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
