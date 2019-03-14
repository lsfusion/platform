package lsfusion.server.data.query;

public interface MStaticExecuteEnvironment extends TypeEnvironment {

    void add(StaticExecuteEnvironment environment);

    void addNoReadOnly();

    void addVolatileStats();

    void addNoPrepare();

    void addNotMaterializable(NotMaterializable table);

    void removeNotMaterializable(NotMaterializable table);

    StaticExecuteEnvironment finish();
}
