package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.ProgressBar;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.StackProgress;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.IsClassField;
import lsfusion.server.data.expr.key.NullableKeyExpr;
import lsfusion.server.data.expr.query.AggrType;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.action.session.table.PropertyChangeTableUsage;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.infer.*;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public abstract class AggregateProperty<T extends PropertyInterface> extends Property<T> {

    protected static void fillDepends(MSet<Property> depends, ImCol<? extends PropertyInterfaceImplement> propImplements) {
        for(PropertyInterfaceImplement propImplement : propImplements)
            propImplement.mapFillDepends(depends);
    }


    protected <I extends PropertyInterface> Inferred<I> inferInnerInterfaceClasses(ImList<PropertyInterfaceImplement<I>> used, final boolean isSelect, final ExClassSet commonValue, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, int skipNotNull, InferType inferType) {
        ImList<ExClassSet> valueClasses = ListFact.toList(used.size(), i -> isSelect && i == 0 ? commonValue : ExClassSet.notNull(commonValue));
        return inferInnerInterfaceClasses(used, orders, ordersNotNull, skipNotNull, valueClasses, inferType);
    }

    protected <I extends PropertyInterface> Inferred<I> inferInnerInterfaceClasses(ImList<PropertyInterfaceImplement<I>> used, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, int skipNotNull, ImList<ExClassSet> valueClasses, InferType inferType) {
        return op(used.addList(orders.keyOrderSet()), valueClasses.addList(ListFact.toList(ExClassSet.NULL, orders.size())),
                used.size() + (ordersNotNull ? orders.size() : 0), skipNotNull, inferType, false);
    }

    protected <I extends PropertyInterface> ExClassSet inferInnerValueClass(ImList<PropertyInterfaceImplement<I>> props, ImOrderMap<PropertyInterfaceImplement<I>,Boolean> orders, ImMap<I, ExClassSet> inferred, AggrType aggrType, InferType inferType) {
        ExClassSet valueClass = aggrType.getMain(props, orders).mapInferValueClass(inferred, inferType);
        if(aggrType.isSelect())
            return valueClass;
        assert valueClass != null || inferType == InferType.resolve();

        return ExClassSet.toExType(aggrType.getType(ExClassSet.fromExType(valueClass)));
    }

    protected boolean calculateHasGlobalPreread(boolean events) {
        for(Property property : getDepends(events))
            if(property.hasGlobalPreread(events))
                return true;
        return false;
    }

    public ImSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getDepends());
    }

    protected boolean calculateHasPreread(StructChanges structChanges) {
        for (Property property : getDepends())
            if (property.hasPreread(structChanges))
                return true;
        return false;
    }

    public boolean calculateCheckRecursions(Set<Property<?>> path, Set<Property<?>> localMarks, Set<Property<?>> marks, boolean usePrev) {
        for(Property<?> depend : getDepends())
            if(depend.checkRecursions(path, localMarks, marks, usePrev))
                return true;
        return false;
    }

    public boolean isStored() {
        assert (field!=null) == (mapTable!=null);
        return mapTable!=null && !DataSession.reCalculateAggr; // для тестирования 2-е условие
    }

    protected AggregateProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        super(caption,interfaces);
    }

    public String checkMaterialization(SQLSession session, BaseClass baseClass, boolean runInTransaction) throws SQLException, SQLHandledException {
        return checkMaterialization(session, baseClass, null, runInTransaction);
    }

    // проверяет агрегацию для отладки
    @ThisMessage
    @StackProgress
    public String checkMaterialization(SQLSession session, BaseClass baseClass, @StackProgress ProgressBar progressBar, boolean runInTransaction) throws SQLException, SQLHandledException {
        QueryEnvironment env = DataSession.emptyEnv(OperationOwner.unknown);

        session.pushVolatileStats(OperationOwner.unknown);
        
        try {

            boolean useRecalculate = Settings.get().isUseRecalculateClassesInsteadOfInconsisentExpr();

            String message = "";

            String checkClasses = "";
            if(useRecalculate)
                checkClasses = checkClasses(session, runInTransaction, env, baseClass);

            Query<T, String> checkQuery = getCheckQuery(baseClass, !useRecalculate);

            Result<ImOrderMap<ImMap<T, Object>, ImMap<String, Object>>> rCheckResult = new Result<>();
            DBManager.run(session, runInTransaction, DBManager.CHECK_MAT_TIL, sql -> rCheckResult.set(checkQuery.execute(sql, env)));
            ImOrderMap<ImMap<T, Object>, ImMap<String, Object>> checkResult = rCheckResult.result;
            if(!checkResult.isEmpty() || !checkClasses.isEmpty()) {
                message += "---- Checking Materializations : " + this + "-----" + '\n';
                message += checkClasses;
                for(int i=0,size=checkResult.size();i<size;i++)
                    message += "Keys : " + checkResult.getKey(i) + " : " + checkResult.getValue(i) + '\n';
            }

            return message;
        } finally {
            session.popVolatileStats(OperationOwner.unknown);
        }
    }

    private Query<T, String> getCheckQuery(BaseClass baseClass, boolean checkInconsistence) {
        return getRecalculateQuery(true, true, baseClass, checkInconsistence, null);
    }

    private Query<T, String> getRecalculateQuery(boolean outDB, boolean onlyChanges, BaseClass baseClass, boolean checkInconsistence, PropertyChange<T> where) {
        assert isStored();
        
        QueryBuilder<T, String> query = new QueryBuilder<>(this);
        if(where != null)
            query.and(where.getWhere(query.getMapExprs()));

        Expr dbExpr = checkInconsistence ? getInconsistentExpr(query.getMapExprs(), baseClass) : getExpr(query.getMapExprs());
        Expr calculateExpr = aspectCalculateExpr(query.getMapExprs(), isFullProperty() ? CalcType.RECALC : CalcType.EXPR, PropertyChanges.EMPTY, null);  // оптимизация - только для FULL свойств, так как в остальных лучше использовать кэш по EXPR
        if(outDB)
            query.addProperty("dbvalue", dbExpr);
        query.addProperty(onlyChanges && !outDB ? "value" : "calcvalue", calculateExpr); // value - the same as in PropertyChangeTable
        query.and(dbExpr.getWhere().or(calculateExpr.getWhere()));
        
        if(onlyChanges || !DBManager.RECALC_REUPDATE)
            query.and(dbExpr.compare(calculateExpr, Compare.EQUALS).not().and(dbExpr.getWhere().or(calculateExpr.getWhere())));
        return query.getQuery();
    }

    private boolean isFullProperty() {
        IsClassField fullField;
        return (fullField = mapTable.table.getFullField()) != null && BaseUtils.hashEquals(fullField.getField(), field);
    }

    public static AggregateProperty recalculate = null;

    public void recalculateMaterialization(BusinessLogics BL, DataSession session, SQLSession sql, boolean runInTransaction, BaseClass baseClass) throws SQLException, SQLHandledException {
        recalculateMaterialization(BL, session, sql, baseClass, null, null, runInTransaction);
    }

    public void recalculateMaterialization(BusinessLogics BL, DataSession session, SQLSession sql, BaseClass baseClass, PropertyChange<T> where, Boolean recalculateClasses, boolean runInTransaction) throws SQLException, SQLHandledException {
        recalculateMaterialization(sql, runInTransaction, baseClass, where, recalculateClasses);

        ObjectValue propertyObject = BL.reflectionLM.propertyCanonicalName.readClasses(session, new DataObject(getCanonicalName()));
        if (propertyObject instanceof DataObject)
            BL.reflectionLM.lastRecalculateProperty.change(LocalDateTime.now(), session, (DataObject) propertyObject);
    }

    @StackMessage("{logics.info.recalculation.of.materialized.property}")
    @ThisMessage
    public void recalculateMaterialization(SQLSession sql, boolean runInTransaction, BaseClass baseClass, PropertyChange<T> where, Boolean recalculateClasses) throws SQLException, SQLHandledException {
        boolean classesAreValid = recalculateClasses != null && !recalculateClasses; // NOCLASSES
        if (!classesAreValid && (recalculateClasses != null || Settings.get().isUseRecalculateClassesInsteadOfInconsisentExpr())) {
            recalculateClasses(sql, runInTransaction, baseClass, where);

            classesAreValid = true;
        }

        if(recalculateClasses == null || !recalculateClasses) { // ALL || NOCLASSES
            boolean serializable = DBManager.RECALC_MAT_TIL;
            boolean mixedSerializable = runInTransaction && !serializable && Settings.get().isRecalculateMaterializationsMixedSerializable();

            Query<T, String> recalculateQuery = getRecalculateQuery(false, mixedSerializable, baseClass, !classesAreValid, where);

            OperationOwner opOwner = OperationOwner.unknown;
            sql.pushVolatileStats(opOwner);

            PropertyChangeTableUsage<T> table = null;
            try {
                if (mixedSerializable) {
                    table = createChangeTable("recmt");
                    table.writeRows(sql, recalculateQuery, baseClass, DataSession.emptyEnv(opOwner), false);
                    recalculateQuery = getRecalculateQuery(false, false, baseClass, !classesAreValid, PropertyChangeTableUsage.getChange(table));
                    serializable = true;
                }

                ModifyQuery modifyQuery = new ModifyQuery(mapTable.table, recalculateQuery.map(mapTable.mapKeys.reverse(), MapFact.singletonRev(field, "calcvalue")), opOwner, TableOwner.global);
                DBManager.run(sql, runInTransaction, serializable, session -> session.modifyRecords(modifyQuery));
            } finally {
                if (table != null)
                    table.drop(sql, opOwner);

                sql.popVolatileStats(opOwner);
            }
        }
    }
    
    @IdentityStartLazy
    private Pair<ImRevMap<T,NullableKeyExpr>, Expr> calculateQueryExpr(CalcType calcType) {
        ImRevMap<T,NullableKeyExpr> mapExprs = getMapNotNullKeys();
        return new Pair<>(mapExprs, calculateExpr(mapExprs, calcType));
    }
    
    @IdentityStartLazy
    public ClassWhere<Object> calcClassValueWhere(CalcClassType calcType) {
        Pair<ImRevMap<T, NullableKeyExpr>, Expr> query = calculateQueryExpr(calcType == CalcClassType.prevSame() && noOld() ? CalcClassType.prevBase() : calcType); // оптимизация
        ClassWhere<Object> result = Query.getClassWhere(Where.TRUE(), query.first, MapFact.singleton((Object) "value", query.second)); 
        if(calcType == CalcClassType.prevSame()) // для того чтобы докинуть orAny, собсно только из-за этого infer необходим в любом случае
            result = result.and(inferClassValueWhere(InferType.prevSame()));
        return result;
    }

    @Override
    @IdentityLazy
    public boolean calcNotNull(ImSet<T> checkInterfaces, CalcInfoType calcType) {
        Pair<ImRevMap<T, NullableKeyExpr>, Expr> query = calculateQueryExpr(calcType); // оптимизация
        return query.second.getWhere().means(Expr.getWhere(query.first));
    }

    @Override
    @IdentityLazy
    public boolean calcEmpty(CalcInfoType calcType) {
        return calculateQueryExpr(calcType).second.getWhere().isFalse();
    }
}
