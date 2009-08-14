package platform.server.session;

import platform.server.data.classes.CustomClass;
import platform.server.logics.properties.DataProperty;

public abstract class DataChanges<This extends DataChanges<This>> {

    public abstract void add(This changes);

    public abstract void dependsAdd(This changes, CustomClass customClass);
    public abstract void dependsRemove(This changes, CustomClass customClass);
    public abstract void dependsData(This changes, DataProperty property);

    public abstract boolean hasChanges();
}
