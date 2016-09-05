package lsfusion.server.logics.tasks.impl.recalculate;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.table.ImplementTable;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.session.DataSession;
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
        assert element instanceof CalcProperty;
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
        } else if(property instanceof CalcProperty) {
            result = DataSession.checkClasses((CalcProperty) property, sql, getBL().LM.baseClass);
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
        elements.addAll(getBL().getStoredDataProperties().toJavaList());
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
            stat = element instanceof ImplementTable ? ((ImplementTable) element).getStatRows() : element instanceof CalcProperty ?
                    ((CalcProperty) element).mapTable.table.getStatProps().get(((CalcProperty) element).field).notNull : Stat.MAX;
        } catch (Exception e) {
            stat = null;
        }
        return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
    }
}
