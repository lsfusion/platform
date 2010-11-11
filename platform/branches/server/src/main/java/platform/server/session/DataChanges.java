package platform.server.session;

import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.UserProperty;

// вообще должен содержать только DataProperty и ActionProperty но так как мн-вого наследования нету приходится извращаться
public class DataChanges extends AbstractPropertyChanges<ClassPropertyInterface, UserProperty, DataChanges> {

    protected DataChanges createThis() {
        return new DataChanges();
    }

    public DataChanges() {
    }

    public DataChanges(UserProperty property, PropertyChange<ClassPropertyInterface> change) {
        super(property, change);
    }

    private DataChanges(DataChanges changes1, DataChanges changes2) {
        super(changes1, changes2);
    }

    public DataChanges add(DataChanges add) {
        return new DataChanges(this, add);
    }

}
