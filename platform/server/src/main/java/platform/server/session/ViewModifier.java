package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.classes.ValueClass;
import platform.server.classes.CustomClass;

public abstract class ViewModifier implements Modifier<ViewDataChanges> {
    public ViewDataChanges view;

    public ViewModifier(ViewDataChanges view) {
        this.view = view;
    }

    public ViewDataChanges newChanges() {
        return new ViewDataChanges();
    }

    public void modifyAdd(ViewDataChanges changes, ValueClass customClass) {
        if(customClass instanceof CustomClass && view.addClasses.contains((CustomClass)customClass))
            changes.addClasses.add((CustomClass)customClass);
    }

    public void modifyRemove(ViewDataChanges changes, ValueClass customClass) {
        if(customClass instanceof CustomClass && view.removeClasses.contains((CustomClass)customClass))
            changes.removeClasses.add((CustomClass)customClass);
    }

    public void modifyData(ViewDataChanges changes, DataProperty property) {
        if(view.properties.contains(property))
            changes.properties.add(property);
    }
}
