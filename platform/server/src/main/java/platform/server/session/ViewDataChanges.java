package platform.server.session;

import platform.server.data.classes.CustomClass;
import platform.server.logics.properties.DataProperty;

import java.util.HashSet;
import java.util.Set;

public class ViewDataChanges implements DataChanges {

    public Set<DataProperty> properties = new HashSet<DataProperty>();

    public Set<CustomClass> addClasses = new HashSet<CustomClass>();
    public Set<CustomClass> removeClasses = new HashSet<CustomClass>();

    public Set<DataProperty> getProperties() {
        return properties;
    }

    public Set<CustomClass> getAddClasses() {
        return addClasses;
    }

    public Set<CustomClass> getRemoveClasses() {
        return removeClasses;
    }

    public boolean hasChanges() {
        return !(properties.isEmpty() && addClasses.isEmpty() && removeClasses.isEmpty());
    }

    ViewDataChanges copy() {
        ViewDataChanges copyChanges = new ViewDataChanges();
        copyChanges.properties.addAll(properties);
        copyChanges.addClasses.addAll(addClasses);
        copyChanges.removeClasses.addAll(removeClasses);
        return copyChanges;
    }
}
