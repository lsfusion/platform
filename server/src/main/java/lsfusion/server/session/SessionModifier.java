package lsfusion.server.session;

import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.ValuesContext;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.OverrideSessionModifier;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// поддерживает hint'ы, есть информация о сессии
public abstract class SessionModifier implements Modifier {

    private WeakIdentityHashSet<OverrideSessionModifier> views = new WeakIdentityHashSet<OverrideSessionModifier>();
    public void registerView(OverrideSessionModifier modifier) { // protected
        views.add(modifier);
        modifier.eventDataChanges(getPropertyChanges().getProperties());
    }

    public void unregisterView(OverrideSessionModifier modifier) { // protected
        views.remove(modifier);
    }

    protected void eventDataChanges(ImSet<? extends CalcProperty> properties, FunctionSet<? extends CalcProperty> sourceChanged) {
        assert sourceChanged.isFull() || (((ImSet<CalcProperty>)properties).containsAll((ImSet<CalcProperty>)sourceChanged));
        for(CalcProperty property : properties)
            eventChange(property, true, ((FunctionSet<CalcProperty>) sourceChanged).contains(property)); // как правило этот метод используется для сброса изменений, поэтому пометим что все изменилось
    }

    protected void eventDataChanges(ImSet<? extends CalcProperty> properties) {
        eventDataChanges(properties, FullFunctionSet.<CalcProperty>instance());
    }

    private MSet<CalcProperty> mChanged = SetFact.mSet();

    protected void eventChange(CalcProperty property, boolean data, boolean source) {
        if(source)
            mChanged.add(property);
/*        else {
            if(!mChanged.contains(property)) {
                ModifyChange modifyChange = getModifyChange(property);
                if(!BaseUtils.nullEquals(modifyChange, propertyChanges.getModify(property)))
                    modifyChange = modifyChange;
            }
        }*/

        if(data) { // если изменились данные, drop'аем хинты
            try {
                for(CalcProperty<?> incrementProperty : getIncrementProps()) {
                    if(CalcProperty.depends(incrementProperty, property)) {
                        if(increment.contains(incrementProperty))
                            increment.remove(incrementProperty, getSQL());
                        preread.remove(incrementProperty);
                        eventChange(incrementProperty, false, true); // так как изначально итерация идет или по increment или по preread, сработает в любом случае
                    }
                }
                MAddSet<CalcProperty> removedNoUpdate = SetFact.mAddSet();
                for(CalcProperty<?> incrementProperty : noUpdate)
                    if(CalcProperty.depends(incrementProperty, property)) // сбрасываем noUpdate, уведомляем остальных об изменении
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

    protected void eventNoUpdate(CalcProperty property) {
        mChanged.add(property);

        for(OverrideSessionModifier view : views)
            view.eventChange(property, true, true); // если сюда зашли, значит гарантировано изменили данные
    }

    protected void eventSourceChanges(Iterable<? extends CalcProperty> properties) {
        for(CalcProperty property : properties)
            eventChange(property, false, true); // используется только в случаях когда гарантировано меняется "источник"
    }


    // по сути protected
    protected PropertyChanges propertyChanges = PropertyChanges.EMPTY;
    @ManualLazy
    public PropertyChanges getPropertyChanges() {
        ImSet<CalcProperty> changed = mChanged.immutable();
        if(changed.size()>0) {
            ImMap<CalcProperty, ModifyChange> replace = changed.mapValues(new GetValue<ModifyChange, CalcProperty>() {
                public ModifyChange getMapValue(CalcProperty value) {
                    return getModifyChange(value);
                }});

//            if(!calculatePropertyChanges().equals(propertyChanges.replace(replace)))
//                mChanged = mChanged;

            propertyChanges = propertyChanges.replace(replace);
            mChanged = SetFact.mSet();
        }
        return propertyChanges;
    }

    public ImSet<CalcProperty> getHintProps() {
        return noUpdate.immutableCopy().merge(getIncrementProps());
    }

    private CalcProperty readProperty;
    private Set<CalcProperty> prereadProps = new HashSet<CalcProperty>();

    // hint'ы хранит
    private TableProps increment = new TableProps();
    private Map<CalcProperty, PrereadRows> preread = new HashMap<CalcProperty, PrereadRows>();
    private ImSet<CalcProperty> getPrereadProps() {
        return SetFact.fromJavaSet(preread.keySet());
    }
    private ImSet<CalcProperty> getIncrementProps() {
        return increment.getProperties().merge(getPrereadProps());
    }

    public void clearHints(SQLSession session) throws SQLException {
        eventSourceChanges(getIncrementProps());
        increment.clear(session);
        preread.clear();
        eventDataChanges(noUpdate.immutableCopy());
        noUpdate = SetFact.mAddSet();
    }

    public void clearPrereads() throws SQLException {
        eventSourceChanges(getPrereadProps());
        preread.clear();
    }

    public abstract SQLSession getSQL();
    public abstract BaseClass getBaseClass();
    public abstract QueryEnvironment getQueryEnv();

    public boolean allowHintIncrement(CalcProperty property) {
        if (increment.contains(property))
            return false;

        if (readProperty != null && readProperty.equals(property))
            return false;

        if (!property.allowHintIncrement())
            return false;

        return true;
    }

    public boolean forceHintIncrement(CalcProperty property) {
        return false;
    }

    public boolean allowNoUpdate(CalcProperty property) {
        return !noUpdate.contains(property) && !forceDisableNoUpdate(property);
    }

    public boolean forceNoUpdate(CalcProperty property) {
        return false;
    }

    protected <P extends PropertyInterface> boolean allowPropertyPrereadValues(CalcProperty<P> property) {
        if(!property.complex)
            return false;

        if(Settings.get().isDisablePrereadValues())
            return false;

        if (prereadProps.contains(property))
            return false;

        return true;
    }

    public <P extends PropertyInterface> ValuesContext cacheAllowPrereadValues(CalcProperty<P> property) {
        if(!allowPropertyPrereadValues(property))
            return null;

        PrereadRows prereadRows = preread.get(property);
        if(prereadRows==null)
            return PrereadRows.EMPTY();

        return prereadRows;
    }

    // assert что в values только
    public <P extends PropertyInterface> boolean allowPrereadValues(CalcProperty<P> property, ImMap<P, Expr> values) {
        // assert что values только complex values

        if(!allowPropertyPrereadValues(property))
            return false;

        PrereadRows prereadRows = preread.get(property);

        if(values.size()==property.interfaces.size()) { // если все есть
            if(prereadRows!=null && prereadRows.readValues.containsKey(values))
                return false;
        } else {
            ImMap<P, Expr> complexValues = CalcProperty.onlyComplex(values);
            if(complexValues.isEmpty() || (prereadRows!=null && prereadRows.readParams.keys().containsAll(complexValues.values().toSet())))
                return false;
        }

        return true;
    }

    public boolean forceDisableNoUpdate(CalcProperty property) {
        return true;
    }

    public int getLimitHintIncrementComplexity() {
        return Settings.get().getLimitHintIncrementComplexity();
    }

    public int getLimitGrowthIncrementComplexity() {
        return Settings.get().getLimitGrowthIncrementComplexity();
    }

    public int getLimitHintIncrementStat() {
        return Settings.get().getLimitHintIncrementStat();
    }

    public int getLimitHintNoUpdateComplexity() {
        return Settings.get().getLimitHintNoUpdateComplexity();
    }

    public void addHintIncrement(CalcProperty property) throws SQLException, SQLHandledException {
        assert allowHintIncrement(property);

        try {
            readProperty = property;
            increment.add(property, property.readChangeTable(getSQL(), this, getBaseClass(), getQueryEnv()));
        } catch(Exception e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        } finally {
            readProperty = null;
        }

        eventChange(property, false, true); // используется только в случаях когда гарантировано меняется "источник"
    }

    public <P extends PropertyInterface> void addPrereadValues(CalcProperty<P> property, ImMap<P, Expr> values) throws SQLException, SQLHandledException {
        assert property.complex && allowPrereadValues(property, values);

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
                readedParamValues = Expr.readValues(getSQL(), getBaseClass(), valueSet.toMap(), getQueryEnv());
            else
                readedParamValues = MapFact.EMPTY();

            ImMap<ImMap<P, Expr>, Pair<ObjectValue, Boolean>> readValues;
            if(values.size() == property.interfaces.size())
                readValues = MapFact.singleton(values, property.readClassesChanged(getSQL(), values.join(prereadedParamValues.addExcl(readedParamValues)), getBaseClass(), this, getQueryEnv()));
            else
                readValues = MapFact.EMPTY();

            PrereadRows<P> readRows = new PrereadRows<P>(readedParamValues, readValues);
            if(prereadRows != null)
                readRows = prereadRows.addExcl(readRows);

            preread.put(property, readRows);
        } finally {
            prereadProps.remove(property);
        }

        eventChange(property, false, true); // используется только в случаях когда гарантировано меняется "источник"
    }

    private MAddSet<CalcProperty> noUpdate = SetFact.mAddSet();
    public void addNoUpdate(CalcProperty property) {
        assert allowNoUpdate(property);

        noUpdate.add(property);

        eventNoUpdate(property);
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property) {
        return getModifyChange(property, PrereadRows.<P>EMPTY(), SetFact.<CalcProperty>EMPTY());
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property, PrereadRows<P> preread, FunctionSet<CalcProperty> disableHint) {

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
                return new ModifyChange<P>(change, preread, true);
        }

        return calculateModifyChange(property, preread, disableHint);
    }

    protected abstract <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, PrereadRows<P> preread, FunctionSet<CalcProperty> overrided);

    public ImSet<CalcProperty> getProperties() {
        return getHintProps().merge(calculateProperties());
    }

    public abstract ImSet<CalcProperty> calculateProperties();

    public PropertyChanges calculatePropertyChanges() {
        PropertyChanges result = PropertyChanges.EMPTY;
        for(CalcProperty property : getProperties()) {
            ModifyChange modifyChange = getModifyChange(property);
            if(modifyChange!=null)
                result = result.add(new PropertyChanges(property, modifyChange));
        }
        return result;
    }

    public void clean(SQLSession sql) throws SQLException {
        increment.clear(sql);
        preread.clear();
        assert views.isEmpty();
    }
}
