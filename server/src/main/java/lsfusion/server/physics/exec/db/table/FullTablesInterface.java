package lsfusion.server.physics.exec.db.table;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;

public interface FullTablesInterface {

    ImSet<ImplementTable> getFullTables(ObjectValueClassSet findItem, ImplementTable skipTable);

}
