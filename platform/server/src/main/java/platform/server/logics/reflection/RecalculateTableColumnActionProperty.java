package platform.server.logics.reflection;

import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.ReflectionLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

import static platform.server.logics.ServerResourceBundle.getString;

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

        DataObject tableColumnObject = context.getKeyValue(tableColumnInterface);
        String propertySID = (String) context.getBL().reflectionLM.sidTableColumn.read(context, tableColumnObject);

        sqlSession.startTransaction();
        context.getDbManager().recalculateAggregationTableColumn(sqlSession, propertySID.trim());
        sqlSession.commitTransaction();

        context.delayUserInterfaction(new MessageClientAction(getString("logics.recalculation.was.completed"), getString("logics.recalculation.aggregations")));
    }
}