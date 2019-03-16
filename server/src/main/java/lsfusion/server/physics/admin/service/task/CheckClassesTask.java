package lsfusion.server.physics.admin.service.task;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.antlr.runtime.RecognitionException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CheckClassesTask extends GroupPropertiesSingleTask<Object> { // integer - exclusiveness, table - ключи, property - свойства

    @Override
    public String getTaskCaption(Object element) {
        if (element instanceof Integer) {
            return "Check Exclusiveness";
        } else if (element instanceof ImplementTable) {
            return "Check Table Classes";
        }
        assert element instanceof Property;
        return "Check Class";
    }

    @Override
    protected void runInnerTask(Object property, ExecutionStack stack) throws RecognitionException, SQLException, SQLHandledException {
        SQLSession sql = getDbManager().getThreadLocalSql();
        String result = null;
        if(property instanceof Integer) {
            result = DataSession.checkClasses(sql, getBL().LM.baseClass);
        } else if (property instanceof ImplementTable) {
            result = DataSession.checkTableClasses((ImplementTable) property, sql, getBL().LM.baseClass, false); // так как снизу есть проверка классов
        } else if(property instanceof Property) {
            result = DataSession.checkClasses((Property) property, sql, getBL().LM.baseClass);
        }
        if (result != null && !result.isEmpty())
            addMessage(result);
    }

    @Override
    protected List getElements() {
        checkContext();
        List elements = new ArrayList();
        elements.add(1);
        elements.addAll(getBL().LM.tableFactory.getImplementTables().toJavaSet());
        try(DataSession session = createSession()) {
            elements.addAll(getBL().getStoredDataProperties(session).toJavaList());
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
        return elements;
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ImplementTable ? ((ImplementTable) element).getName() :
                element instanceof Property ? ((Property) element).getSID() : null;
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
            stat = element instanceof ImplementTable ? ((ImplementTable) element).getStatRows() : element instanceof Property ?
                    ((Property) element).mapTable.table.getStatProps().get(((Property) element).field).notNull : Stat.MAX;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}
