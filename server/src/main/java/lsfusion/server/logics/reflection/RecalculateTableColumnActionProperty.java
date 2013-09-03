package lsfusion.server.logics.reflection;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ReflectionLogicsModule;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.logics.ServerResourceBundle.getString;

public class RecalculateTableColumnActionProperty extends ScriptingActionProperty {

    private final ClassPropertyInterface tableColumnInterface;

    public RecalculateTableColumnActionProperty(ReflectionLogicsModule LM) {
        super(LM, new ValueClass[]{LM.getClassByName("TableColumn")});
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        tableColumnInterface = i.next();
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        SQLSession sqlSession = context.getSession().sql;

        DataObject tableColumnObject = context.getDataKeyValue(tableColumnInterface);
        String propertySID = (String) context.getBL().reflectionLM.sidTableColumn.read(context, tableColumnObject);

        try {
            sqlSession.startTransaction();
            context.getDbManager().recalculateAggregationTableColumn(sqlSession, propertySID.trim());
            sqlSession.commitTransaction();
        } catch (SQLException e) {
            sqlSession.rollbackTransaction();
        }

        context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
    }
}