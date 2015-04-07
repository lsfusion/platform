package lsfusion.server.data.query;

// Mutable !!! нужен Thread Safe
public abstract class DynamicExecuteEnvironment {

    public abstract DynamicExecEnvSnapshot getSnapshot(int transactTimeout);

    public abstract void succeeded(DynamicExecEnvSnapshot snapshot);

    public abstract void failed(DynamicExecEnvSnapshot snapshot);

    public final static DynamicExecuteEnvironment DEFAULT = new DynamicExecuteEnvironment() {
        public DynamicExecEnvSnapshot getSnapshot(int transactTimeout) {
            return new DynamicExecEnvSnapshot(false, 0, transactTimeout);
        }

        public void succeeded(DynamicExecEnvSnapshot snapshot) {
        }

        public void failed(DynamicExecEnvSnapshot snapshot) {
            assert false; // по идее такого не может быть
        }
    };
}
