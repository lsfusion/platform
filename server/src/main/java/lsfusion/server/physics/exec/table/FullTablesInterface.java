package lsfusion.server.physics.exec.table;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ObjectValueClassSet;

public interface FullTablesInterface {

    ImSet<ImplementTable> getFullTables(ObjectValueClassSet findItem, ImplementTable skipTable);

}
