package lsfusion.server.logics.tasks;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.cases.graph.Graph;

import java.util.List;

public abstract class GroupGraphTask<T> extends GroupPropertiesSingleTask<T> {

    private Graph<T> graph;

    protected abstract Graph<T> getGraph(BusinessLogics<?> BL);

    @Override
    protected List<T> getElements() {
        checkContext();

        graph = getGraph(getBL());
        return graph.getNodes().toOrderSet().toJavaList();
    }

    @Override
    protected ImSet<T> getDependElements(T key) {
        return graph.getEdgesOut(key);
    }

}
