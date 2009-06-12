package platform.server.session;

import platform.server.data.classes.CustomClass;
import platform.server.logics.properties.DataProperty;

import java.util.Set;

// изменения данных
public interface DataChanges {

    Set<DataProperty> getProperties();

    Set<CustomClass> getAddClasses();
    Set<CustomClass> getRemoveClasses();

    boolean hasChanges();
}
