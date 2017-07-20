package lsfusion.server.logics.table;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.ObjectValueClassSet;
import lsfusion.server.classes.ValueClass;

public interface FullTablesInterface {

    ImSet<ImplementTable> getFullTables(ObjectValueClassSet findItem, ImplementTable skipTable);

}
