package platform.server.session;

import platform.server.classes.CustomClass;
import platform.server.logics.property.DataProperty;

import java.util.HashSet;
import java.util.Set;

public class ViewChanges implements Changes<ViewChanges> {

    public ViewChanges() {
    }
    
    public ViewChanges(SessionChanges changes) {
        properties = new HashSet<DataProperty>(changes.data.keySet());
        addClasses = new HashSet<CustomClass>(changes.add.keySet());
        removeClasses = new HashSet<CustomClass>(changes.remove.keySet());
    }

    public ViewChanges(ViewChanges changes) {
        properties = new HashSet<DataProperty>(changes.properties);
        addClasses = new HashSet<CustomClass>(changes.addClasses);
        removeClasses = new HashSet<CustomClass>(changes.removeClasses);
    }

    public Set<DataProperty> properties = new HashSet<DataProperty>();

    public Set<CustomClass> addClasses = new HashSet<CustomClass>();
    public Set<CustomClass> removeClasses = new HashSet<CustomClass>();

    public void add(ViewChanges changes) {
        properties.addAll(changes.properties);
        addClasses.addAll(changes.addClasses);
        removeClasses.addAll(changes.removeClasses);
    }

    public boolean hasChanges() {
        return !properties.isEmpty() || !addClasses.isEmpty() || !removeClasses.isEmpty();
    }
}
