package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.classes.ValueClass;
import platform.server.classes.CustomClass;

public abstract class ViewModifier implements Modifier<ViewChanges> {
    public ViewChanges view;

    public ViewModifier(ViewChanges view) {
        this.view = view;
    }

    public ViewChanges newChanges() {
        return new ViewChanges();
    }

    public void modifyAdd(ViewChanges changes, ValueClass customClass) {
        if(customClass instanceof CustomClass && view.addClasses.contains((CustomClass)customClass))
            changes.addClasses.add((CustomClass)customClass);
    }

    public void modifyRemove(ViewChanges changes, ValueClass customClass) {
        if(customClass instanceof CustomClass && view.removeClasses.contains((CustomClass)customClass))
            changes.removeClasses.add((CustomClass)customClass);
    }

    public void modifyData(ViewChanges changes, DataProperty property) {
        if(view.properties.contains(property))
            changes.properties.add(property);
    }
}
