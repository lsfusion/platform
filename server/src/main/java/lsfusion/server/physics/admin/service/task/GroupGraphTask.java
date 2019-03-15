package lsfusion.server.physics.admin.service.task;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;

import java.sql.SQLException;
import java.util.List;

public abstract class GroupGraphTask<T> extends GroupPropertiesSingleTask<T> {

    private Graph<T> graph;

    protected abstract Graph<T> getGraph(DataSession session, BusinessLogics BL) throws SQLException, SQLHandledException;

    @Override
    protected List<T> getElements() {
        checkContext();

        try(DataSession session = createSession()) {
            graph = getGraph(session, getBL());
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
        return graph.getNodes().toOrderSet().toJavaList();
    }

    @Override
    protected ImSet<T> getDependElements(T key) {
        return graph.getEdgesOut(key);
    }

}
