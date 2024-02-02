package lsfusion.server.physics.admin.service.task;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;

import java.sql.SQLException;
import java.util.List;

public class CheckMaterializationsTask extends GroupPropertiesSingleTask<AggregateProperty> {
    @Override
    protected void runInnerTask(AggregateProperty element, ExecutionStack stack) throws SQLException, SQLHandledException {
        SQLSession sql = getDbManager().getThreadLocalSql();
        String result = element.checkMaterialization(sql, getBL().LM.baseClass);
        if (result != null && !result.isEmpty())
            addMessage(result);
    }

    @Override
    protected List<AggregateProperty> getElements() {
        checkContext();
        try(DataSession dataSession = createSession()) {
            return getBL().getRecalculateAggregateStoredProperties(dataSession, false);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String getTaskCaption(AggregateProperty element) {
        return "Check Materializations";
    }

    @Override
    protected String getElementCaption(AggregateProperty element) {
        return element.getSID();
    }

    @Override
    protected ImSet<AggregateProperty> getDependElements(AggregateProperty key) {
        return SetFact.EMPTY();
    }
}
