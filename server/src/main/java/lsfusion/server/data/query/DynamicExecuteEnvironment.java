package lsfusion.server.data.query;

import lsfusion.server.Settings;
import lsfusion.server.data.SQLCommand;
import lsfusion.server.form.navigator.SQLSessionUserProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Mutable !!! нужен Thread Safe
public abstract class DynamicExecuteEnvironment<OE, S extends DynamicExecEnvSnapshot<OE, S>> {

    public abstract S getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<OE, S> outerEnv); // nullable последний параметр

    public abstract void succeeded(SQLCommand command, S snapshot, long l);

    public abstract TypeExecuteEnvironment getType();

    public abstract void failed(SQLCommand command, S snapshot);

    public final static DynamicExecuteEnvironment<Object, AdjustVolatileExecuteEnvironment.Snapshot> DEFAULT = new DynamicExecuteEnvironment<Object, AdjustVolatileExecuteEnvironment.Snapshot>() {
        public AdjustVolatileExecuteEnvironment.Snapshot getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<Object, AdjustVolatileExecuteEnvironment.Snapshot> outerEnv) {
            return new AdjustVolatileExecuteEnvironment.Snapshot(false, 0, transactTimeout);
        }

        public TypeExecuteEnvironment getType() {
            return TypeExecuteEnvironment.NONE;
        }

        public DynamicExecEnvOuter<Object, AdjustVolatileExecuteEnvironment.Snapshot> createOuter(Object env) {
            return null;
        }

        public void succeeded(SQLCommand command, AdjustVolatileExecuteEnvironment.Snapshot snapshot, long l) {
        }

        public void failed(SQLCommand command, AdjustVolatileExecuteEnvironment.Snapshot snapshot) {
            assert false; // по идее такого не может быть
        }
    };

    private static Map<Integer, Integer> userExecEnvs = new ConcurrentHashMap<>();

    public static void setUserExecEnv(Integer user, Integer type) {
        if(type == null)
            userExecEnvs.remove(user);
        else
            userExecEnvs.put(user, type);
    }

    public static Integer getUserExecEnv(SQLSessionUserProvider userProvider) {
        return userExecEnvs.get(userProvider.getCurrentUser());
    }

    public static <OE, S extends DynamicExecEnvSnapshot<OE, S>> DynamicExecEnvOuter<OE, S> create(final OE outerEnv) {
        return new DynamicExecEnvOuter<OE, S>() {
            public OE getOuter() {
                return outerEnv; // nullable
            }

            public S getSnapshot() {
                return null;
            }
        };
    }

}
