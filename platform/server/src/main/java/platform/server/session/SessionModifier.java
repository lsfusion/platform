package platform.server.session;

import platform.base.EmptyFunctionSet;
import platform.base.FunctionSet;
import platform.base.QuickSet;
import platform.base.WeakIdentityHashSet;
import platform.server.caches.ManualLazy;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.OverrideSessionModifier;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

    private QuickSet<CalcProperty> changed = new QuickSet<CalcProperty>();

    protected void eventDataChange(CalcProperty property) {
        changed.add(property);

        // если increment использовал property drop'аем hint
        try {
            for(CalcProperty<?> incrementProperty : new QuickSet<CalcProperty>(increment.getProperties())) {
                if(CalcProperty.depends(incrementProperty, property)) {
                    increment.remove(incrementProperty, getSQL());
                    eventSourceChange(incrementProperty);
                }
            }
            QuickSet<CalcProperty> removedNoUpdate = new QuickSet<CalcProperty>();
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
        changed.add(property);

        for(OverrideSessionModifier view : views)
            view.eventDataChange(property);
    }

    protected void eventSourceChange(CalcProperty property) {
        changed.add(property);

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
        if(changed.size>0) {
            Map<CalcProperty, ModifyChange> replace = new HashMap<CalcProperty, ModifyChange>();
            for(int i=0;i<changed.size;i++) {
                CalcProperty property = changed.get(i);
                replace.put(property, getModifyChange(property));
            }

//            if(!calculatePropertyChanges().equals(propertyChanges.replace(replace)))
//                changed = changed;

            propertyChanges = propertyChanges.replace(replace);
            changed = new QuickSet<CalcProperty>();
        }
        return propertyChanges;
    }

    public QuickSet<CalcProperty> getHintProps() {
        return noUpdate.merge(increment.getProperties());
    }

    CalcProperty readProperty;

    // hint'ы хранит
    private TableProps increment = new TableProps();

    public void clearHints(SQLSession session) throws SQLException {
        eventSourceChanges(increment.getProperties());
        increment.clear(session);
        eventDataChanges(noUpdate);
        noUpdate = new QuickSet<CalcProperty>();
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

    public boolean allowNoUpdate(CalcProperty property) {
        return !noUpdate.contains(property) && !forceDisableNoUpdate(property);
    }

    public boolean forceNoUpdate(CalcProperty property) {
        return false;
    }

    public boolean forceDisableNoUpdate(CalcProperty property) {
        return true;
    }

    private QuickSet<CalcProperty> noUpdate = new QuickSet<CalcProperty>();
    public void addNoUpdate(CalcProperty property) {
        assert allowNoUpdate(property);

        noUpdate.add(property);

        eventNoUpdate(property);
    }

    public <P extends PropertyInterface> ModifyChange<P> getModifyChange(CalcProperty<P> property) {
        return getModifyChange(property, EmptyFunctionSet.<CalcProperty>instance());
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

    public QuickSet<CalcProperty> getProperties() {
        return getHintProps().merge(calculateProperties());
    }

    public abstract QuickSet<CalcProperty> calculateProperties();

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
