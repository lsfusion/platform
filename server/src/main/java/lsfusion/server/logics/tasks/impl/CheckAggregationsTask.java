package lsfusion.server.logics.tasks.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.PublicTask;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static lsfusion.base.BaseUtils.serviceLogger;

public class CheckAggregationsTask extends GroupPropertiesSingleTask{
    public List<String> messages = new ArrayList<>();
    private Set<AggregateProperty> notRecalculateSet;

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        this.messages = new ArrayList<>();
        notRecalculateSet = context.getBL().getNotRecalculateAggregateStoredProperties();
        setBL(context.getBL());
        setDependencies(new HashSet<PublicTask>());
    }

    @Override
    protected void runTask(final Object property) throws RecognitionException {
        if (property instanceof AggregateProperty && !notRecalculateSet.contains(property)) {
            try (DataSession session = getDbManager().createSession()) {
                long start = System.currentTimeMillis();
                String result = ((AggregateProperty) property).checkAggregation(session.sql, session.env, getBL().LM.baseClass);
                if (result != null && !result.isEmpty())
                    messages.add(result);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Check Aggregations: %s, %sms", ((AggregateProperty) property).getSID(), time));
            } catch (SQLException | SQLHandledException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected List getElements() {
        return getBL().getAggregateStoredProperties(false);
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof AggregateProperty ? ((AggregateProperty) element).getSID() : null;
    }

    @Override
    protected String getErrorsDescription(Object element) {
        return "";
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        return SetFact.EMPTY();
    }
}
