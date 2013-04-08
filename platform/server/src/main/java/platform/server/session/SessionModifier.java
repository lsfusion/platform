package platform.server.session;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.WeakIdentityHashSet;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.add.MAddSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.Settings;
import platform.server.caches.ManualLazy;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.OverrideSessionModifier;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;

import static platform.base.BaseUtils.merge;

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
            for(CalcProperty<?> incrementProperty : increment.getProperties()) {
                if(CalcProperty.depends(incrementProperty, property)) {
                    increment.remove(incrementProperty, getSQL());
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
        return noUpdate.immutableCopy().merge(increment.getProperties());
    }
    
    public FunctionSet<CalcProperty> getUsedHints() { // для кэширования
        return merge(noUpdate.immutableCopy(), increment.getProperties(), readProperty != null ? SetFact.singleton(readProperty) : SetFact.<CalcProperty>EMPTY());
    }

    private CalcProperty readProperty;

    // hint'ы хранит
    private TableProps increment = new TableProps();

    public void clearHints(SQLSession session) throws SQLException {
        eventSourceChanges(increment.getProperties());
        increment.clear(session);
        eventDataChanges(noUpdate);
        noUpdate = SetFact.mAddSet();
    }

    public abstract SQLSession getSQL();
    public abstract BaseClass getBaseClass();
    public abstract QueryEnvironment getQueryEnv();

    public boolean allowHintIncrement(CalcProperty property) {
        if (increment.contains(property))
            return false;

        if (readProperty != null && readProperty.equals(property))
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

    private MAddSet<CalcProperty> noUpdate = SetFact.mAddSet();
    public void addNoUpdate(CalcProperty property) {
        assert allowNoUpdate(property);

        noUpdate.add(property);

        eventNoUpdate(property);
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property) {
        return getModifyChange(property, SetFact.<CalcProperty>EMPTY());
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property, FunctionSet<CalcProperty> disableHint) {
        if(noUpdate.contains(property) && !disableHint.contains(property))
            return new ModifyChange<P>(property.getNoChange(), true);

        PropertyChange<P> change = increment.getPropertyChange(property);
        if(change!=null && !disableHint.contains(property))
            return new ModifyChange<P>(change, true);

        return calculateModifyChange(property, disableHint);
    }

    protected abstract <P extends PropertyInterface> ModifyChange<P> calculateModifyChange(CalcProperty<P> property, FunctionSet<CalcProperty> overrided);

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
        assert views.isEmpty();
    }
}
