package lsfusion.server.logics.classes.user;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.E2Runnable;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.IsClassField;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClassSet;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;

import java.sql.SQLException;

import static lsfusion.server.physics.admin.log.ServerLoggers.runWithServiceLog;

public interface ObjectValueClassSet extends ObjectClassSet, ValueClassSet {

    long getCount();

    int getClassCount();

    ImSet<ConcreteCustomClass> getSetConcreteChildren();

    String getWhereString(String source);

    String getNotWhereString(String source);

    BaseClass getBaseClass();

    boolean hasComplex();

    ImRevMap<ObjectClassField, ObjectValueClassSet> getObjectClassFields(); // CustomClass только как хранитель таблицы

    ImRevMap<IsClassField, ObjectValueClassSet> getIsClassFields(); // CustomClass только как хранитель таблицы

    ImRevMap<IsClassField, ObjectValueClassSet> getClassFields(boolean onlyObjectClassFields); // по сути protected

    default void recalculateClassStat(BaseLogicsModule LM, DataSession session, String logSuffix) throws SQLException, SQLHandledException {
        runWithServiceLog((E2Runnable<SQLException, SQLHandledException>) () -> {
            recalculateClassStat(LM, session);
        }, String.format("Recalculate Class Stats%s: %s", BaseUtils.nvl(logSuffix,"") , this));
    }

    default void recalculateClassStat(BaseLogicsModule LM, DataSession session) throws SQLException, SQLHandledException {
        QueryBuilder<Integer, Integer> classes = new QueryBuilder<>(SetFact.singleton(0));

        KeyExpr countKeyExpr = new KeyExpr("count");
        Expr countExpr = GroupExpr.create(MapFact.singleton(0, countKeyExpr.classExpr(LM.baseClass)),
                ValueExpr.COUNT, countKeyExpr.isClass(ObjectValueClassSet.this), GroupType.SUM, classes.getMapExprs());

        classes.addProperty(0, countExpr);
        classes.and(countExpr.getWhere());

        ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> classStats = classes.execute(session);
        ImSet<ConcreteCustomClass> concreteChilds = getSetConcreteChildren();
        for (int i = 0, size = concreteChilds.size(); i < size; i++) {
            ConcreteCustomClass customClass = concreteChilds.get(i);
            ImMap<Integer, Object> classStat = classStats.get(MapFact.singleton(0, customClass.ID));
            LM.statCustomObjectClass.change(classStat == null ? 1 : Stat.restrictLongValuesInStat((Long) classStat.singleValue()), session, customClass.getClassObject());
        }
    }

}
