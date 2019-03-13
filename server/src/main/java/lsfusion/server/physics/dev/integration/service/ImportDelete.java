package lsfusion.server.physics.dev.integration.service;

import lsfusion.server.logics.property.CalcPropertyImplement;
import lsfusion.server.logics.property.PropertyInterface;

public class ImportDelete <P extends PropertyInterface, T extends PropertyInterface> {
    ImportKey<P> key;

    CalcPropertyImplement<T, ImportDeleteInterface> deleteProperty;
    boolean deleteAll;

    public ImportDelete(ImportKey<P> key, CalcPropertyImplement<T, ImportDeleteInterface> deleteProperty, boolean deleteAll) {
        this.key = key;
        this.deleteProperty = deleteProperty;
        this.deleteAll = deleteAll;
    }
}
