package lsfusion.server.logics.tasks.impl;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.QueryBuilder;
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

import static lsfusion.base.BaseUtils.serviceLogger;

public class RecalculateStatsTask extends GroupPropertiesSingleTask {

    public void init(ExecutionContext context) throws SQLException, SQLHandledException {
        setBL(context.getBL());
        setDependencies(new HashSet<PublicTask>());
    }

    @Override
    protected void runTask(final Object element) throws RecognitionException {
        try (DataSession session = getDbManager().createSession()) {
            if(element instanceof ImplementTable) {
                long start = System.currentTimeMillis();
                serviceLogger.info(String.format("Recalculate Stats %s", element));
                ((ImplementTable) element).calculateStat(getBL().reflectionLM, session);
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Recalculate Stats: %s, %sms", element, time));
            } else if(element instanceof ObjectValueClassSet) {
                long start = System.currentTimeMillis();
                serviceLogger.info(String.format("Recalculate Stats: %s", element));
                QueryBuilder<Integer, Integer> classes = new QueryBuilder<>(SetFact.singleton(0));

                KeyExpr countKeyExpr = new KeyExpr("count");
                Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(getBL().LM.baseClass)),
                        new ValueExpr(1, IntegerClass.instance), countKeyExpr.isClass((ObjectValueClassSet) element), GroupType.SUM, classes.getMapExprs());

                classes.addProperty(0, countExpr);
                classes.and(countExpr.getWhere());

                ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);
                ImSet<ConcreteCustomClass> concreteChilds = ((ObjectValueClassSet) element).getSetConcreteChildren();
                for (int i = 0, size = concreteChilds.size(); i < size; i++) {
                    ConcreteCustomClass customClass = concreteChilds.get(i);
                    ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, (Object) customClass.ID));
                    getBL().LM.statCustomObjectClass.change(classStat == null ? 1 : (Integer) classStat.singleValue(), session, customClass.getClassObject());
                }
                long time = System.currentTimeMillis() - start;
                serviceLogger.info(String.format("Recalculate Stats: %s, %sms", element, time));
            }
            session.apply(getBL());
        } catch (SQLException | SQLHandledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List getElements() {
        List elements = new ArrayList();
        elements.addAll(getBL().LM.tableFactory.getImplementTables().toJavaSet());
        elements.addAll(getBL().LM.baseClass.getUpObjectClassFields().values().toJavaCol());
        return elements;
    }

    @Override
    protected String getElementCaption(Object element) {
        return element instanceof ImplementTable ? ((ImplementTable) element).getName() :
                element instanceof ObjectValueClassSet ? String.valueOf(element) : null;
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
        if (element instanceof ImplementTable) {
            Stat stat = ((ImplementTable) element).getStatKeys().rows;
            return stat == null ? Stat.MIN.getWeight() : stat.getWeight();
        } else if (element instanceof ObjectValueClassSet)
            return ((ObjectValueClassSet) element).getCount();
        else
            return Stat.MIN.getWeight();
    }
}
