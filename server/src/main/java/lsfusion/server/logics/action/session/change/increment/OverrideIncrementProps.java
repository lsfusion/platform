package lsfusion.server.logics.action.session.change.increment;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class OverrideIncrementProps extends IncrementProps {

    public IncrementProps override;
    public IncrementProps increment;

    public OverrideIncrementProps(IncrementProps override, IncrementProps increment) {
        this.override = override;
        this.increment = increment;

        override.registerView(this);
        increment.registerView(this);
    }

    public void clean() {
        override.unregisterView(this);
        increment.unregisterView(this);
    }

    @Override
    public <P extends PropertyInterface> PropertyChange<P> getPropertyChange(CalcProperty<P> property) {
        PropertyChange<P> overrideChange = override.getPropertyChange(property);
        if(overrideChange!=null)
            return overrideChange;
        return increment.getPropertyChange(property);
    }

    @Override
    public ImSet<CalcProperty> getProperties() {
        return override.getProperties().merge(increment.getProperties());
    }

    public long getMaxCount(CalcProperty property) {
        return BaseUtils.max(override.getMaxCount(property), increment.getMaxCount(property));
    }

    @Override
    public String out() {
        return "\noverride : " + BaseUtils.tab(override.out()) + "\nincrement : " + BaseUtils.tab(increment.out());
    }
}
