package lsfusion.server.logics.action.session.change.modifier;

import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.caches.ValuesContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.data.PrereadRows;
import lsfusion.server.logics.action.session.change.*;
import lsfusion.server.logics.action.session.changed.UpdateResult;
import lsfusion.server.logics.action.session.table.PropertyChangeTableUsage;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// поддерживает hint'ы, есть информация о сессии
public abstract class SessionModifier implements Modifier {

    private final String debugInfo;
    
    public SessionModifier(String debugInfo) {
        this.debugInfo = debugInfo;
    }

    private ConcurrentIdentityWeakHashSet<OverrideSessionModifier> views = new ConcurrentIdentityWeakHashSet<>();
    public void registerView(OverrideSessionModifier modifier) throws SQLException, SQLHandledException { // protected
        views.add(modifier);
        modifier.eventDataChanges(getPropertyChanges().getProperties());
    }

    public long getMaxCountUsed(Property<?> property) {
        long result = 0;
        for(Property depend : property.getRecDepends()) {
            result = BaseUtils.max(result, getMaxCount(depend));
        }
        return result;
    }
    public abstract long getMaxCount(Property recDepends);

    public void unregisterView(OverrideSessionModifier modifier) { // protected
        views.remove(modifier);
    }

    public <P extends Property> boolean eventChanges(ImSet<P> properties, Function<P, ? extends UpdateResult> modifyResults) throws SQLException, SQLHandledException {
        boolean dataChanged = false;
        for(P property : properties) {
            UpdateResult result = modifyResults.apply(property);
            if(result.dataChanged())
                dataChanged = true;
            eventChange(property, result.dataChanged(), result.sourceChanged());
        }
        return dataChanged;
    }

    public <P extends Property> void eventDataChanges(ImSet<P> properties) throws SQLException, SQLHandledException {
        eventChanges(properties, ModifyResult.DATA_SOURCE.fnGetValue());
    }

    protected void eventChange(Property property, boolean data, boolean source) throws SQLHandledException {
        if(source)
            addChange(property, data);
/*        else {
            if(!mChanged.contains(property)) {
                ModifyChange modifyChange = getModifyChange(property);
                if(!BaseUtils.nullEquals(modifyChange, propertyChanges.getModify(property)))
                    modifyChange = modifyChange;
            }
        }*/

        if(data) { // если изменились данные, drop'аем хинты
            try {
                for(Property<?> incrementProperty : getIncrementProps()) {
                    if(Property.depends(incrementProperty, property)) {
                        if(increment.contains(incrementProperty))
                            increment.remove(incrementProperty, getSQL(), getOpOwner());
                        preread.remove(incrementProperty);
                        eventChange(incrementProperty, false, true); // так как изначально итерация идет или по increment или по preread, сработает в любом случае
                    }
                }
                MAddSet<Property> removedNoUpdate = SetFact.mAddSet();
                for(Property<?> incrementProperty : noUpdate)
                    if(Property.depends(incrementProperty, property)) // сбрасываем noUpdate, уведомляем остальных об изменении
                        eventNoUpdate(incrementProperty);
                    else
                        removedNoUpdate.add(incrementProperty);
                noUpdate = removedNoUpdate;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        
        for(OverrideSessionModifier view : views)
            view.eventChange(property, data, source);
    }
    
    protected void notifySourceChange(ImMap<Property, Boolean> changed, boolean forceUpdate) throws SQLException, SQLHandledException {        
    }

    protected void eventNoUpdate(Property property) throws SQLException, SQLHandledException {
        addChange(property, true);

        for(OverrideSessionModifier view : views)
            view.eventChange(property, true, true); // если сюда зашли, значит гарантировано изменили данные
    }

    protected void eventSourceChanges(Iterable<? extends Property> properties) throws SQLException, SQLHandledException {
        for(Property property : properties)
            eventChange(property, false, true); // используется только в случаях когда гарантировано меняется "источник"
    }


    private MMap<Property, Boolean> mChanged = null;
    private void addChange(Property property, boolean dataChanged) {
        if(mChanged == null)
            mChanged = MapFact.mMap(MapFact.or());
        mChanged.add(property, dataChanged);
    }

    public PropertyChanges getPropertyChanges() throws SQLException, SQLHandledException {
        return getPropertyChanges(false);
    }
    
    private static boolean enableCheckChanges = false; 

    private ImSet<Property> propertyChangesRecursionGuard = SetFact.EMPTY();
    // по сути protected
    protected PropertyChanges propertyChanges = PropertyChanges.EMPTY;
    @ManualLazy
    public PropertyChanges getPropertyChanges(boolean forceUpdate) throws SQLException, SQLHandledException {
        if(mChanged != null) {
            ImMap<Property, Boolean> changed = mChanged.immutable();
            assert !changed.isEmpty();
            mChanged = null;
            
            ImMap<Property, ModifyChange> replace = changed.keys().mapValues((Function<Property, ModifyChange>) this::getModifyChange);

            if(enableCheckChanges && !forceUpdate && !calculatePropertyChanges().equals(propertyChanges.replace(replace))) // может нарушаться если в calculatePropertyChanges кто-то empty возвращает (а в replace есть filter not empty)
                assert false;

            propertyChanges = propertyChanges.replace(replace);
        
            ImSet<Property> prevRecursionGuard = propertyChangesRecursionGuard;
            propertyChangesRecursionGuard = propertyChangesRecursionGuard.merge(changed.keys());
            try {
                ImMap<Property, Boolean> guardedChanged = changed.remove(prevRecursionGuard);
//                if(guardedChanged.size() < changed.size())
//                    ServerLoggers.exinfoLog("GUARDED CHANGES : " + changed + ", " + prevRecursionGuard);
                notifySourceChange(guardedChanged, forceUpdate);

                return getPropertyChanges(forceUpdate); // так как source change мог еще раз изменить
            } finally {
                propertyChangesRecursionGuard = prevRecursionGuard;
            }
        }
        return propertyChanges;
    }

    public void updateSourceChanges() throws SQLException, SQLHandledException {
        getPropertyChanges(true);
        for(OverrideSessionModifier view : views)
            view.updateSourceChanges();        
    }

    public ImSet<Property> getHintProps() {
        return noUpdate.immutableCopy().merge(getIncrementProps());
    }

    private Property readProperty;
    public Set<Property> prereadProps = new HashSet<>();

    // hint'ы хранит
    private TableProps increment = new TableProps();
    private Map<Property, PrereadRows> preread = new HashMap<>();
    private ImSet<Property> getPrereadProps() {
        return SetFact.fromJavaSet(preread.keySet());
    }
    private ImSet<Property> getIncrementProps() {
        return increment.getProperties().merge(getPrereadProps());
    }

    // должно цеплять все views, чтобы не получилось что increment'ы создались до начала транзакции, а удалялись по eventChange (который цепляет все views), тогда rollbacktransaction вернет назад записи в старые таблицы
    public void clearHints(SQLSession session, OperationOwner owner) throws SQLException, SQLHandledException {
        eventSourceChanges(getIncrementProps());
        increment.clear(session, owner);
        preread.clear();
        eventDataChanges(noUpdate.immutableCopy());
        noUpdate = SetFact.mAddSet();

        for(OverrideSessionModifier view : views)
            view.clearHints(session, owner);
    }

    public void clearPrereads() throws SQLException, SQLHandledException {
        eventSourceChanges(getPrereadProps());
        preread.clear();
    }

    public abstract SQLSession getSQL();
    public abstract BaseClass getBaseClass();
    public abstract QueryEnvironment getQueryEnv();
    
    public abstract OperationOwner getOpOwner();

    public boolean allowHintIncrement(Property property) {
        if (increment.contains(property))
            return false;

        if (readProperty != null && readProperty.equals(property))
            return false;

        if (!property.allowHintIncrement())
            return false;

        return true;
    }

    public boolean forceHintIncrement(Property property) {
        return false;
    }

    public boolean allowNoUpdate(Property property) {
        return !noUpdate.contains(property) && !forceDisableNoUpdate(property);
    }

    public boolean forceNoUpdate(Property property) {
        return false;
    }

    protected <P extends PropertyInterface> boolean allowPropertyPrereadValues(Property<P> property) {
        if(Settings.get().isDisablePrereadValues())
            return false;

        if (prereadProps.contains(property))
            return false;

        return true;
    }

    public <P extends PropertyInterface> ValuesContext cacheAllowPrereadValues(Property<P> property) {
        if(!allowPropertyPrereadValues(property))
            return null;

        PrereadRows prereadRows = preread.get(property);
        if(prereadRows==null)
            return PrereadRows.EMPTY();

        return prereadRows;
    }

    public <P extends PropertyInterface> boolean forcePrereadValues(Property<P> property) {
        return property.isPreread();
    }

    // assert что в values только
    // предполагается что должно быть consistent с MapCacheAspect.prereadHintEnabled
    public <P extends PropertyInterface> boolean allowPrereadValues(Property<P> property, ImMap<P, Expr> values) {
        // assert что values только complex values

        if(!allowPropertyPrereadValues(property))
            return false;

        PrereadRows prereadRows = preread.get(property);

        if(values.size()==property.interfaces.size()) { // если все есть
            if(prereadRows!=null && prereadRows.readValues.containsKey(values))
                return false;
        } else {
            ImMap<P, Expr> complexValues = Property.onlyComplex(values);
            if(complexValues.isEmpty() || (prereadRows!=null && prereadRows.readParams.keys().containsAll(complexValues.values().toSet())))
                return false;
        }

        return true;
    }

    public boolean forceDisableNoUpdate(Property property) {
        return true;
    }

    public int getLimitHintIncrementComplexity() {
        return Settings.get().getLimitHintIncrementComplexityCoeff();
    }

    public int getLimitHintIncrementValueComplexity() {
        return Settings.get().getLimitHintIncrementValueComplexityCoeff();
    }

    public double getLimitComplexityGrowthCoeff() {
        return Settings.get().getLimitComplexityGrowthCoeff();
    }

    public long getLimitHintIncrementStat() {
        return Settings.get().getLimitHintIncrementStat();
    }

    public int getLimitHintNoUpdateComplexity() {
        return Settings.get().getLimitHintNoUpdateComplexity();
    }

    public void addHintIncrement(Property property) throws SQLException, SQLHandledException {
        assert allowHintIncrement(property);

        try {
            readProperty = property;
            final PropertyChangeTableUsage changeTable = property.readChangeTable("htincr", getSQL(), this, getBaseClass(), getQueryEnv());
            increment.add(property, changeTable);
        } catch(Exception e) {
            String message = e.getMessage();
            if(message != null && message.contains("does not exist")) // выводим, что за modifier
                SQLSession.outModifier("DOES NOT EXIST", this);
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        } finally {
            readProperty = null;
        }

        eventChange(property, false, true); // используется только в случаях когда гарантировано меняется "источник"
    }

    public <P extends PropertyInterface> void addPrereadValues(Property<P> property, ImMap<P, Expr> values) throws SQLException, SQLHandledException {
        assert property.isPreread() && allowPrereadValues(property, values);

        try {
            prereadProps.add(property);

            PrereadRows<P> prereadRows = preread.get(property);

            ImSet<Expr> valueSet = values.values().toSet();
            ImMap<Expr, ObjectValue> prereadedParamValues;
            if(prereadRows!=null) {
                prereadedParamValues = prereadRows.readParams.filter(valueSet);
                valueSet = valueSet.remove(prereadedParamValues.keys());
            } else
                prereadedParamValues = MapFact.EMPTY();

            ImMap<Expr, ObjectValue> readedParamValues;
            if(!valueSet.isEmpty())
                readedParamValues = Expr.readObjectValues(getSQL(), getBaseClass(), valueSet.toMap(), getQueryEnv());
            else
                readedParamValues = MapFact.EMPTY();

            ImMap<ImMap<P, Expr>, Pair<ObjectValue, Boolean>> readValues;
            if(values.size() == property.interfaces.size())
                readValues = MapFact.singleton(values, property.readClassesChanged(getSQL(), values.join(prereadedParamValues.addExcl(readedParamValues)), getBaseClass(), this, getQueryEnv()));
            else
                readValues = MapFact.EMPTY();

            PrereadRows<P> readRows = new PrereadRows<>(readedParamValues, readValues);
            if(prereadRows != null)
                readRows = prereadRows.addExcl(readRows);

            preread.put(property, readRows);
        } finally {
            prereadProps.remove(property);
        }

        eventChange(property, false, true); // используется только в случаях когда гарантировано меняется "источник"
    }

    private MAddSet<Property> noUpdate = SetFact.mAddSet();
    public void addNoUpdate(Property property) throws SQLException, SQLHandledException {
        assert allowNoUpdate(property);

        noUpdate.add(property);

        eventNoUpdate(property);
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(Property<P> property) {
        return getModifyChange(property, PrereadRows.EMPTY(), SetFact.EMPTY());
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(Property<P> property, PrereadRows<P> preread, FunctionSet<Property> disableHint) {

        if(!disableHint.contains(property)) {
            PrereadRows<P> rows = this.preread.get(property);
            if(rows!=null)
                preread = preread.add(rows);

            PropertyChange<P> change;
            if(noUpdate.contains(property))
                change = property.getNoChange();
            else
                change = increment.getPropertyChange(property);

            if(change!=null)
                return new ModifyChange<>(change, preread, true);
        }

        return calculateModifyChange(property, preread, disableHint);
    }

    protected abstract <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(Property<P> property, PrereadRows<P> preread, FunctionSet<Property> overrided);

    public ImSet<Property> getProperties() {
        return getHintProps().merge(calculateProperties());
    }

    public abstract ImSet<Property> calculateProperties();

    public PropertyChanges calculatePropertyChanges() {
        PropertyChanges result = PropertyChanges.EMPTY;
        for(Property property : getProperties()) {
            ModifyChange modifyChange = getModifyChange(property);
            if(modifyChange!=null)
                result = result.add(new PropertyChanges(property, modifyChange));
        }
        return result;
    }

    public boolean checkPropertyChanges() throws SQLException, SQLHandledException {
        return BaseUtils.hashEquals(getPropertyChanges(), calculatePropertyChanges());
    }

    public void clean(SQLSession sql, OperationOwner opOwner) throws SQLException {
        increment.clear(sql, opOwner);
        preread.clear();
        assert views.isEmpty();
    }
    
    public void cleanViews() { // нужен для того чтобы очистить views раньше и не синхронизировать тогда clean и eventChange
        assert views.isEmpty();
    }

    @Override
    public String toString() {
        return debugInfo;
    }
    
    public String out() {
        return '\n' + debugInfo + "\nincrement : " + BaseUtils.tab(increment.out());
    }
}
