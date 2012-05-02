package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.server.classes.ValueClass;
import platform.server.session.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public abstract class ExecuteProperty extends UserProperty {

    protected ExecuteProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    public boolean isStored() {
        return false;
    }

    // assert что возвращает только DataProperty и IsClassProperty
    public abstract Set<Property> getChangeProps();
    public abstract Set<Property> getUsedProps();

    public boolean pendingEventExecute() {
        return getChangeProps().size()==0;
    }

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        assert getDepends().isEmpty();
        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
        for(Property depend : getUsedProps())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.USEDACTION));
        for(Property depend : getEventDepends())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.EVENTACTION));
        return BaseUtils.merge(result, super.calculateLinks());
    }

    public PropertyChange<ClassPropertyInterface> getEventAction(Modifier modifier) {
        return getEventAction(modifier.getPropertyChanges());
    }

    public PropertyChange<ClassPropertyInterface> getEventAction(PropertyChanges changes) {
        return event.getDataChanges(changes).get(this);
    }
}
