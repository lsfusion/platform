package lsfusion.server.logics.tasks.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.table.ImplementTable;
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

public class CheckClassesTask extends GroupPropertiesSingleTask{
    public List<String> messages = new ArrayList<>();
    private Set<AggregateProperty> notRecalculateSet;

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        messages = new ArrayList<>();
        notRecalculateSet = context.getBL().getNotRecalculateAggregateStoredProperties();
        setBL(context.getBL());
        setDependencies(new HashSet<PublicTask>());
    }

    @Override
    protected void runTask(final Object property) throws RecognitionException {
        try (DataSession session = getDbManager().createSession()) {
            long start = System.currentTimeMillis();
            if(property instanceof Integer) {
                serviceLogger.info("Check common classes");
                String result = DataSession.checkClasses(session.sql, session.env, getBL().LM.baseClass);
                if (result != null && !result.isEmpty())
                    messages.add(result);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Check common classes: %sms", time));
            } else if (property instanceof ImplementTable) {
                serviceLogger.info(String.format("Check Table Classes: %s", ((ImplementTable) property).getName()));
                String result = DataSession.checkTableClasses((ImplementTable) property, session.sql, session.env, getBL().LM.baseClass);
                if(result != null && !result.isEmpty())
                    messages.add(result);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Check Table Classes: %s, %sms", ((ImplementTable) property).getName(), time));
            } else if(property instanceof CalcProperty && !notRecalculateSet.contains(property)) {
                serviceLogger.info(String.format("Check Classes: %s", ((CalcProperty) property).getSID()));
                String result = DataSession.checkClasses((CalcProperty) property, session.sql, session.env, getBL().LM.baseClass);
                if(result != null && !result.isEmpty())
                    messages.add(result);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Check Classes: %s, %sms", ((CalcProperty) property).getSID(), time));
            }
        } catch (SQLException | SQLHandledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List getElements() {
        List elements = new ArrayList();
        elements.add(1);
        elements.addAll(getBL().LM.tableFactory.getImplementTables().toJavaSet());
        elements.addAll(getBL().getStoredDataProperties(false).toJavaList());
        return elements;
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ImplementTable ? ((ImplementTable) element).getName() :
                element instanceof CalcProperty ? ((CalcProperty) element).getSID() : null;
    }

    @Override
    protected String getErrorsDescription(Object element) {
        return "";
    }

    @Override
    protected ImSet<Object> getDependElements(Object key) {
        return SetFact.EMPTY();
    }

    @Override
    protected long getTaskComplexity(Object element) {
        Stat stat;
        try {
            stat = element instanceof ImplementTable ? ((ImplementTable) element).getStatKeys().rows : element instanceof CalcProperty ?
                    ((CalcProperty) element).mapTable.table.getStatProps().get(((CalcProperty) element).field).notNull : Stat.MAX;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}
