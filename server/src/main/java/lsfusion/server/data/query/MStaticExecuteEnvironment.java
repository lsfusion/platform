package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImSet;

public interface MStaticExecuteEnvironment extends TypeEnvironment {

    void add(StaticExecuteEnvironment environment);

    void addNoReadOnly();

    void addVolatileStats();

    void addNoPrepare();

    void addNotMaterializable(NotMaterializable table);

    void removeNotMaterializable(NotMaterializable table);

    StaticExecuteEnvironment finish();
}
