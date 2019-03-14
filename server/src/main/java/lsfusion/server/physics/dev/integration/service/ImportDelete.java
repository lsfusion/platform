package lsfusion.server.physics.dev.integration.service;

import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

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
