package platform.server.integration;

import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.SessionTableUsage;

public class ImportDelete <P extends PropertyInterface, T extends PropertyInterface> {
    ImportKey<P> key;

    PropertyImplement<T, ImportDeleteInterface> deleteProperty;
    boolean deleteAll;

    public ImportDelete(ImportKey<P> key, PropertyImplement<T, ImportDeleteInterface> deleteProperty, boolean deleteAll) {
        this.key = key;
        this.deleteProperty = deleteProperty;
        this.deleteAll = deleteAll;
    }
}
