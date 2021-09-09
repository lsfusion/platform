package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
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

    protected <I extends PropertyInterface> ExClassSet inferInnerValueClass(ImList<PropertyInterfaceImplement<I>> used, ImMap<I, ExClassSet> inferred, AggrType aggrType, InferType inferType) {
        ExClassSet valueClass = used.get(aggrType.getMainIndex()).mapInferValueClass(inferred, inferType);
        if(aggrType.isSelect())
            return valueClass;

        if(valueClass == null) {
            assert inferType == InferType.resolve();
            return null;
        }
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

    public boolean calculateCheckRecursions(ImSet<CaseUnionProperty> abstractPath, ImSet<Property> path, Set<Property> marks) {
        for(Property depend : getDepends())
            if(depend.checkRecursions(abstractPath, path, marks))
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

    public String checkAggregation(SQLSession session, BaseClass baseClass) throws SQLException, SQLHandledException {
        return checkAggregation(session, null, baseClass, null);
    }

    public String checkAggregation(SQLSession session, BaseClass baseClass, ProgressBar progressBar) throws SQLException, SQLHandledException {
        return checkAggregation(session, null, baseClass, progressBar);
    }

    // проверяет агрегацию для отладки
    @ThisMessage
    @StackProgress
    public String checkAggregation(SQLSession session, QueryEnvironment env, BaseClass baseClass, @StackProgress ProgressBar progressBar) throws SQLException, SQLHandledException {
        session.pushVolatileStats(OperationOwner.unknown);
        
        try {

            boolean useRecalculate = Settings.get().isUseRecalculateClassesInsteadOfInconsisentExpr();

            String message = "";

            String checkClasses = "";
            if(useRecalculate)
                checkClasses = env == null ? DataSession.checkClasses(this, session, baseClass) : DataSession.checkClasses(this, session, env, baseClass);
    
            ImOrderMap<ImMap<T, Object>, ImMap<String, Object>> checkResult = env == null ? getRecalculateQuery(true, baseClass, !useRecalculate).execute(session, OperationOwner.unknown)
                    : getRecalculateQuery(true, baseClass, !useRecalculate).execute(session, env);
            if(checkResult.size() > 0 || !checkClasses.isEmpty()) {
                message += "---- Checking Aggregations : " + this + "-----" + '\n';
                message += checkClasses;
                for(int i=0,size=checkResult.size();i<size;i++)
                    message += "Keys : " + checkResult.getKey(i) + " : " + checkResult.getValue(i) + '\n';
            }

            return message;
        } finally {
            session.popVolatileStats(OperationOwner.unknown);
        }
    }

    private Query<T, String> getRecalculateQuery(boolean outDB, BaseClass baseClass, boolean checkInconsistence) {
        return getRecalculateQuery(outDB, baseClass, checkInconsistence, null);
    }

    private Query<T, String> getRecalculateQuery(boolean outDB, BaseClass baseClass, boolean checkInconsistence, PropertyChange<T> where) {
        assert isStored();
        
        QueryBuilder<T, String> query = new QueryBuilder<>(this);
        if(where != null)
            query.and(where.getWhere(query.getMapExprs()));

        Expr dbExpr = checkInconsistence ? getInconsistentExpr(query.getMapExprs(), baseClass) : getExpr(query.getMapExprs());
        Expr calculateExpr = calculateExpr(query.getMapExprs(), isFullProperty() ? CalcType.RECALC : CalcType.EXPR, PropertyChanges.EMPTY, null);  // оптимизация - только для FULL свойств, так как в остальных лучше использовать кэш по EXPR
        if(outDB)
            query.addProperty("dbvalue", dbExpr);
        query.addProperty("calcvalue", calculateExpr);
        query.and(dbExpr.getWhere().or(calculateExpr.getWhere()));
        
        if(outDB || !DBManager.RECALC_REUPDATE)
            query.and(dbExpr.compare(calculateExpr, Compare.EQUALS).not().and(dbExpr.getWhere().or(calculateExpr.getWhere())));
        return query.getQuery();
    }

    private boolean isFullProperty() {
        IsClassField fullField;
        return (fullField = mapTable.table.getFullField()) != null && BaseUtils.hashEquals(fullField.getField(), field);
    }

    public static AggregateProperty recalculate = null;

    public void recalculateAggregation(BusinessLogics BL, DataSession session, SQLSession sql, BaseClass baseClass) throws SQLException, SQLHandledException {
        recalculateAggregation(BL, session, sql, baseClass, null, true);
    }

    public void recalculateAggregation(BusinessLogics BL, DataSession session, SQLSession sql, BaseClass baseClass, PropertyChange<T> where, boolean recalculateClasses) throws SQLException, SQLHandledException {
        recalculateAggregation(sql, null, baseClass, where, recalculateClasses);

        ObjectValue propertyObject = BL.reflectionLM.propertyCanonicalName.readClasses(session, new DataObject(getCanonicalName()));
        if (propertyObject instanceof DataObject)
            BL.reflectionLM.lastRecalculateProperty.change(LocalDateTime.now(), session, (DataObject) propertyObject);
    }

    @StackMessage("{logics.info.recalculation.of.aggregated.property}")
    @ThisMessage
    public void recalculateAggregation(SQLSession session, QueryEnvironment env, BaseClass baseClass, PropertyChange<T> where, boolean recalculateClasses) throws SQLException, SQLHandledException {
        boolean useRecalculate = recalculateClasses && (Settings.get().isUseRecalculateClassesInsteadOfInconsisentExpr() || where != null);
        if(useRecalculate)
            recalculateClasses(session, env, baseClass);

        session.pushVolatileStats(OperationOwner.unknown);
        try {
            session.modifyRecords(
                    new ModifyQuery(mapTable.table, getRecalculateQuery(false, baseClass, !useRecalculate, where).map(
                            mapTable.mapKeys.reverse(), MapFact.singletonRev(field, "calcvalue")), env == null ? DataSession.emptyEnv(OperationOwner.unknown) : env, TableOwner.global)
                    );
        } finally {
            session.popVolatileStats(OperationOwner.unknown);
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
