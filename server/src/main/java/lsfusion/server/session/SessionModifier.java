package lsfusion.server.session;

import lsfusion.base.FunctionSet;
import lsfusion.base.Pair;
import lsfusion.base.WeakIdentityHashSet;
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

    protected void eventDataChanges(Iterable<? extends CalcProperty> properties) {
        for(CalcProperty property : properties)
            eventDataChange(property);
    }

    private MSet<CalcProperty> mChanged = SetFact.mSet();

    protected void eventDataChange(CalcProperty property) {
        mChanged.add(property);

        // если increment использовал property drop'аем hint
        try {
            for(CalcProperty<?> incrementProperty : getIncrementProps()) {
                if(CalcProperty.depends(incrementProperty, property)) {
                    if(increment.contains(incrementProperty))
                        increment.remove(incrementProperty, getSQL());
                    preread.remove(incrementProperty);
                    eventSourceChange(incrementProperty);
                }
            }
            MAddSet<CalcProperty> removedNoUpdate = SetFact.mAddSet();
            for(CalcProperty<?> incrementProperty : noUpdate)
                if(CalcProperty.depends(incrementProperty, property))
                    eventNoUpdate(incrementProperty);
                else
                    removedNoUpdate.add(incrementProperty);
            noUpdate = removedNoUpdate;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for(OverrideSessionModifier view : views)
            view.eventDataChange(property);
    }

    protected void eventNoUpdate(CalcProperty property) {
        mChanged.add(property);

        for(OverrideSessionModifier view : views)
            view.eventDataChange(property);
    }

    protected void eventSourceChange(CalcProperty property) {
        mChanged.add(property);

        for(OverrideSessionModifier view : views)
            view.eventSourceChange(property);
    }

    protected void eventSourceChanges(Iterable<? extends CalcProperty> properties) {
        for(CalcProperty property : properties)
            eventSourceChange(property);
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
        eventDataChanges(noUpdate);
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

    public void addHintIncrement(CalcProperty property) {
        assert allowHintIncrement(property);

        try {
            readProperty = property;
            increment.add(property, property.readChangeTable(getSQL(), this, getBaseClass(), getQueryEnv()));
            readProperty = null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        eventSourceChange(property);
    }

    public <P extends PropertyInterface> void addPrereadValues(CalcProperty<P> property, ImMap<P, Expr> values) {
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

            prereadProps.remove(property);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        eventSourceChange(property);
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
