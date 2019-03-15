package lsfusion.server.data.query.exec;

import lsfusion.server.data.query.exec.materialize.NotMaterializable;
import lsfusion.server.data.type.exec.TypeEnvironment;

public interface MStaticExecuteEnvironment extends TypeEnvironment {

    void add(StaticExecuteEnvironment environment);

    void addNoReadOnly();

    void addVolatileStats();

    void addNoPrepare();

    void addNotMaterializable(NotMaterializable table);

    void removeNotMaterializable(NotMaterializable table);

    StaticExecuteEnvironment finish();
}
