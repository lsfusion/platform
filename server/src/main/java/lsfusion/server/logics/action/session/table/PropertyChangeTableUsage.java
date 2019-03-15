package lsfusion.server.logics.action.session.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.session.change.Correlation;
import lsfusion.server.logics.action.session.change.ModifyResult;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.classes.changed.RegisterClassRemove;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.BaseClass;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class PropertyChangeTableUsage<K extends PropertyInterface> extends SinglePropertyTableUsage<K> {

    // множественное наследование по сути
    public static class Correlations<K extends PropertyInterface> {
        private final ImRevMap<PropertyField, Correlation<K>> correlations;
        private final SessionTableUsage<K, String> table;

        public Correlations(SessionTableUsage<K, String> table, ImOrderSet<Correlation<K>> correlations) {
            this.correlations = Settings.get().isDisableCorrelations() ? MapFact.<PropertyField, Correlation<K>>EMPTYREV() : genProps(table.mapProps.size(), correlations, new Type.Getter<Correlation<K>>() {
                public Type getType(Correlation<K> key) {
                    return key.getType();
                }
            });
            this.table = table;
        }

        protected ImSet<PropertyField> getFullProps(ImSet<PropertyField> properties) {
            if(!hasCorrelations()) // оптимизация + проверка
                return properties;

            return properties.addExcl(correlations.keys());
        }

        protected IQuery<KeyField, PropertyField> fullMap(final IQuery<KeyField, PropertyField> fieldQuery) {
            if(!hasCorrelations()) // оптимизация + проверка
                return fieldQuery;

            ImRevMap<KeyField, KeyExpr> mapFieldKeys = fieldQuery.getMapKeys();
            QueryBuilder<KeyField, PropertyField> exFieldQuery = new QueryBuilder<>(mapFieldKeys);
            
            exFieldQuery.addProperties(fieldQuery.getProperties().mapValues(new GetValue<Expr, PropertyField>() {
                public Expr getMapValue(PropertyField value) {
                    return fieldQuery.getExpr(value);
                }
            }));
            final ImRevMap<K, KeyExpr> mapKeyExprs = table.mapKeys.crossJoin(mapFieldKeys);
            exFieldQuery.addProperties(correlations.mapValues(new GetValue<Expr, Correlation<K>>() {
                public Expr getMapValue(Correlation<K> value) {
                    return value.getExpr(mapKeyExprs);
                }
            }));
            exFieldQuery.and(fieldQuery.getWhere());
            return exFieldQuery.getQuery();
        }
        protected IQuery<KeyField, PropertyField> fullMap(final IQuery<KeyField, PropertyField> fieldQuery, final Modifier modifier) throws SQLException, SQLHandledException {
            assert hasCorrelations();

            ImRevMap<KeyField, KeyExpr> mapFieldKeys = fieldQuery.getMapKeys();
            QueryBuilder<KeyField, PropertyField> exFieldQuery = new QueryBuilder<>(mapFieldKeys);

            exFieldQuery.addProperties(fieldQuery.getProperties().mapValues(new GetValue<Expr, PropertyField>() {
                public Expr getMapValue(PropertyField value) {
                    return fieldQuery.getExpr(value);
                }
            }));
            final ImRevMap<K, KeyExpr> mapKeyExprs = table.mapKeys.crossJoin(mapFieldKeys);
            exFieldQuery.addProperties(correlations.mapValuesEx(new GetExValue<Expr, Correlation<K>, SQLException, SQLHandledException>() {
                public Expr getMapValue(Correlation<K> value) throws SQLException, SQLHandledException {
                    return value.getExpr(mapKeyExprs, modifier);
                }
            }));
            exFieldQuery.and(fieldQuery.getWhere());
            return exFieldQuery.getQuery();
        }

        protected Join<String> fullJoin(Join<String> result, final Join<PropertyField> join, final ImMap<K, ? extends Expr> joinImplement) {
            if(!hasCorrelations()) // оптимизация + проверка
                return result;

            ImMap<PropertyField, Expr> correlationOut = correlations.mapValues(new GetValue<Expr, Correlation<K>>() {
                public Expr getMapValue(Correlation<K> value) {
                    return value.getExpr(joinImplement);
                }
            });
            ImMap<PropertyField, Expr> correlationIn = correlations.keys().mapValues(new GetValue<Expr, PropertyField>() {
                public Expr getMapValue(PropertyField value) {
                    return join.getExpr(value);
                }
            });
            return result.and(CompareWhere.equalsNull(correlationOut, correlationIn));
        }

        protected ImMap<PropertyField, CustomClass> getInconsistentTableClasses() {
            if(!hasCorrelations()) // оптимизация + проверка
                return MapFact.EMPTY();
                
            return correlations.mapValues(new GetValue<CustomClass, Correlation<K>>() {
                public CustomClass getMapValue(Correlation<K> value) {
                    return value.getCustomClass();
                }
            });
        }

        protected boolean fullHasClassChanges(boolean hasClassChanges, UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
            if(!hasCorrelations()) // optimization + check
                return hasClassChanges;
            
            for(Correlation<K> correlation : correlations.valueIt())
                if(correlation.getProperty().hasChanges(session.modifier))
                    return true;
            return hasClassChanges;                
        }
        
        public boolean hasCorrelations() {
            return !correlations.isEmpty();
        }
    }
    private final Correlations<K> correlations;

    public PropertyChangeTableUsage(ImOrderSet<Correlation<K>> correlations, String debugInfo, ImOrderSet<K> keys, Type.Getter<K> keyType, Type propertyType) {
        super(debugInfo, keys, keyType, propertyType);
        
        this.correlations = new Correlations<>(this, correlations);
        
        initTable(keys);
    }

    @Override
    protected boolean postponeInitTable() {
        return true;
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(PropertyChangeTableUsage<P> table) {
        return SinglePropertyTableUsage.getChange(table);
    }

    // множественное наследование
    @Override
    protected ImSet<PropertyField> getFullProps() {
        return correlations.getFullProps(super.getFullProps());
    }

    @Override
    protected IQuery<KeyField, PropertyField> fullMap(IQuery<K, String> query) {
        return correlations.fullMap(super.fullMap(query));
    }

    @Override
    protected Join<String> fullJoin(final Join<PropertyField> join, final ImMap<K, ? extends Expr> joinImplement) {
        return correlations.fullJoin(super.fullJoin(join, joinImplement), join, joinImplement);
    }

    @Override
    public boolean hasCorrelations() {
        return correlations.hasCorrelations();
    }

    @Override
    protected boolean fullHasClassChanges(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        return correlations.fullHasClassChanges(super.fullHasClassChanges(session), session);
    }

    public ModifyResult modifyRecord(SQLSession session, ImMap<K, DataObject> keyFields, ObjectValue propertyValue, Modify type, OperationOwner owner) throws SQLException, SQLHandledException {
        return modifyRecord(session, keyFields, MapFact.singleton("value", propertyValue), type, owner);
    }

    public ModifyResult modifyRows(SQLSession session, ImRevMap<K, KeyExpr> mapKeys, Expr expr, Where where, BaseClass baseClass, Modify type, QueryEnvironment env, boolean updateClasses) throws SQLException, SQLHandledException {
        return modifyRows(session, new Query<>(mapKeys, expr, "value", where), baseClass, type, env, updateClasses);
    }

    public <B> ClassWhere<B> getClassWhere(ImRevMap<K, ? extends B> remapKeys, B mapProp) {
        return getClassWhere("value", remapKeys, mapProp);
    }

    public void fixKeyClasses(ClassWhere<K> classes) {
        table = table.fixKeyClasses(classes.remap(mapKeys.reverse()), mapProps.singleKey());
    }

    public void checkClasses(SQLSession session, BaseClass baseClass, boolean updateClasses, OperationOwner owner) throws SQLException, SQLHandledException {
        checkClasses(session, baseClass, updateClasses, owner, false, null, null, null, null, null, null, 0);
    }
    // BEFORE TRANSACTION
    public boolean checkClasses(SQLSession sql, BaseClass baseClass, boolean updateClasses, OperationOwner owner, boolean inconsistent, ImMap<K, ValueClass> interfaceClasses, ValueClass valueClass, Result<ImSet<K>> checkKeyChanges, Result<Boolean> checkValueChange, Runnable checkTransaction, RegisterClassRemove classRemove, long timestamp) throws SQLException, SQLHandledException {
        Result<ImSet<Field>> checkChanges = new Result<>();
        
        ImMap<Field, ValueClass> inconsistentTableClasses = null;
        if(inconsistent) {
            inconsistentTableClasses = MapFact.addExcl(mapKeys.join(interfaceClasses), mapProps.singleKey(), valueClass);
            // передаем inconsistent классы для correlations, так как если не будет CONSTRAINT'а на изменение, хотя бы классы не поплывут
            inconsistentTableClasses = MapFact.addExcl(inconsistentTableClasses, correlations.getInconsistentTableClasses());
        }
        SessionData<?> checkedTable = table.checkClasses(sql, baseClass, updateClasses, owner, inconsistent, inconsistentTableClasses, checkChanges, classRemove, timestamp);
        if(inconsistent) {
            checkKeyChanges.set(MapFact.filterRev(mapKeys, checkChanges.result).valuesSet());
            checkValueChange.set(checkChanges.result.contains(mapProps.singleKey()));
        }
        boolean result = !BaseUtils.hashEquals(table, checkedTable);
        if(result && checkTransaction != null)
            checkTransaction.run();
        table = checkedTable;
        return result;
    }

    // тут полагаемся на проверку hasClassChanges, так как изменение корреляций напрямую связано с изменением классов 
    public SessionData updateCorrelations(SessionData<?> table, UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> modifyQuery = new QueryBuilder<>(mapKeys.join(getMapKeys()));
        Join<PropertyField> join = table.join(modifyQuery.getMapExprs());
        modifyQuery.addProperty(mapProps.singleKey(), join.getExpr(mapProps.singleKey()));
        modifyQuery.and(join.getWhere());

        return table.modifyRows(session.sql, correlations.fullMap(modifyQuery.getQuery(), session.modifier), session.baseClass, Modify.UPDATE, session.env, this, new Result<Boolean>(), false); // тут будет избыточная запись UPDATE значений, но строго говоря она же будет избыточной в MODIFY для correlations, когда идет UPDATE (явный UPDATE пока не используется), собственно мы этим и пользуемся
    }
}
