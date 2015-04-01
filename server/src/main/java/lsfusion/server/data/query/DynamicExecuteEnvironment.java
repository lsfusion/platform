package lsfusion.server.data.query;

import lsfusion.server.data.SQLSession;

// Mutable !!! нужен Thread Safe
public abstract class DynamicExecuteEnvironment {

    public abstract DynamicExecEnvSnapshot getInfo(SQLSession session, int transactTimeout);

    public final static DynamicExecuteEnvironment DEFAULT = new DynamicExecuteEnvironment() {
        public DynamicExecEnvSnapshot getInfo(SQLSession session, int transactTimeout) {
            if(session.isInTransaction() && transactTimeout > 0 && !session.isNoHandled() && !session.isNoTransactTimeout())
                return new DynamicExecEnvSnapshot(transactTimeout, true);
            return DynamicExecEnvSnapshot.EMPTY;
        }
    };  
}
