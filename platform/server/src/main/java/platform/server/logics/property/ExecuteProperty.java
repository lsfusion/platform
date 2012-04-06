package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.server.classes.ValueClass;
import platform.server.session.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

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

    public boolean pendingDerivedExecute() {
        return getChangeProps().size()==0;
    }

    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        assert getDepends().isEmpty();
        Collection<Pair<Property<?>, LinkType>> result = new ArrayList<Pair<Property<?>, LinkType>>();
        for(Property depend : getUsedProps())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.ACTIONUSED));
        for(Property depend : getDerivedDepends())
            result.add(new Pair<Property<?>, LinkType>(depend, LinkType.ACTIONDERIVED));
        return BaseUtils.merge(result, super.calculateLinks());
    }

    public PropertyChange<ClassPropertyInterface> getDerivedChange(Modifier modifier) {
        return getDerivedChange(modifier.getPropertyChanges());
    }

    public PropertyChange<ClassPropertyInterface> getDerivedChange(PropertyChanges changes) {
        return derivedChange.getDataChanges(changes).get(this);
    }
}
