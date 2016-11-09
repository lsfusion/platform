package lsfusion.server.data.query;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.type.ArrayClass;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

public interface MStaticExecuteEnvironment extends TypeEnvironment {

    void add(StaticExecuteEnvironment environment);

    void addNoReadOnly();

    void addVolatileStats();

    void addNoPrepare();

    StaticExecuteEnvironment finish();
}
