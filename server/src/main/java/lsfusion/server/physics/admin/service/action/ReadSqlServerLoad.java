package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.server.data.sql.adapter.DataAdapter;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class ReadSqlServerLoad extends InternalAction {

    public ReadSqlServerLoad(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            DataSession session = context.getSession();
            for (DataAdapter.Server server : context.getDbManager().getAdapter().getServers()) {
                Long dbServer = (Long) findProperty("dbServer[STRING]").read(session, new DataObject(server.host));
                LP<?> load = findProperty("load[DBServer]");
                DataObject dbServerClass = new DataObject(dbServer, (ConcreteCustomClass) findClass(server.isMaster() ? "DBMaster" : "DBSlave"));

                load.change(new DataObject(server.getLoad()), session, dbServerClass);
                context.apply();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
