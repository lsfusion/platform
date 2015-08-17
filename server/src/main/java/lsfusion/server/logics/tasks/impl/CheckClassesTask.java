package lsfusion.server.logics.tasks.impl;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
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
    Boolean firstCheck = false;
    private final Object lock = new Object();
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
            if(!firstCheck) {
                synchronized(lock) {
                    firstCheck = true;
                    String result = DataSession.checkClasses(session.sql, session.env, getBL().LM.baseClass);
                    if(result != null && !result.isEmpty())
                        messages.add(result);
                }
            } else if (property instanceof ImplementTable) {
                String result = DataSession.checkTableClasses((ImplementTable) property, session.sql, session.env, getBL().LM.baseClass);
                if(result != null && !result.isEmpty())
                    messages.add(result);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Check Table Classes: %s, %sms", ((ImplementTable) property).getName(), time));
            } else if(property instanceof CalcProperty && !notRecalculateSet.contains(property)) {
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
}
