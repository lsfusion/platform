package platform.server.logics.session;

import platform.server.logics.classes.RemoteClass;
import platform.server.logics.properties.DataProperty;

import java.util.HashSet;
import java.util.Set;

// изменения данных
public class DataChanges {
    public Set<DataProperty> properties = new HashSet<DataProperty>();

    public Set<RemoteClass> addClasses = new HashSet<RemoteClass>();
    public Set<RemoteClass> removeClasses = new HashSet<RemoteClass>();

    DataChanges copy() {
        DataChanges CopyChanges = new DataChanges();
        CopyChanges.properties.addAll(properties);
        CopyChanges.addClasses.addAll(addClasses);
        CopyChanges.removeClasses.addAll(removeClasses);
        return CopyChanges;
    }

    public boolean hasChanges() {
        return !(properties.isEmpty() && addClasses.isEmpty() && removeClasses.isEmpty());
    }
}
