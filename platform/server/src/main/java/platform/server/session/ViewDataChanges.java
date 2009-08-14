package platform.server.session;

import platform.server.data.classes.CustomClass;
import platform.server.logics.properties.DataProperty;
import platform.base.BaseUtils;

import java.util.HashSet;
import java.util.Set;

public class ViewDataChanges extends DataChanges<ViewDataChanges> {

    public Set<DataProperty> properties = new HashSet<DataProperty>();

    public Set<CustomClass> addClasses = new HashSet<CustomClass>();
    public Set<CustomClass> removeClasses = new HashSet<CustomClass>();

    public ViewDataChanges() {
    }

    public ViewDataChanges(TableChanges changes) {
        properties = new HashSet<DataProperty>(changes.data.keySet());
        addClasses = new HashSet<CustomClass>(changes.add.keySet());
        removeClasses = new HashSet<CustomClass>(changes.remove.keySet());
    }

    public void add(ViewDataChanges changes) {
        properties.addAll(changes.properties);
        addClasses.addAll(changes.addClasses);
        removeClasses.addAll(changes.removeClasses);
    }

    public void dependsAdd(ViewDataChanges changes, CustomClass customClass) {
        if(changes.addClasses.contains(customClass))
            addClasses.add(customClass);
    }

    public void dependsRemove(ViewDataChanges changes, CustomClass customClass) {
        if(changes.removeClasses.contains(customClass))
            removeClasses.add(customClass);
    }

    public void dependsData(ViewDataChanges changes, DataProperty property) {
        if(changes.properties.contains(property))
            properties.add(property);
    }

    public boolean hasChanges() {
        return !properties.isEmpty() || !addClasses.isEmpty() || !removeClasses.isEmpty();
    }
}
