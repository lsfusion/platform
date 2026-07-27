package lsfusion.server.data;

import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.base.controller.thread.AssertSynchronized;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;

public abstract class OperationOwner {
    
    // another session's work inside a transaction is not just a nested apply : it runs in that physical transaction (its changes disappear with the outer rollback, it sees uncommitted data,
    // its failure aborts the transaction, its temp tables die with it while their names stay in the pool - see #1716), while its own isInTransaction is false and it believes it is outside one
    @AssertSynchronized
    public void checkThreadSafeAccess(OperationOwner writeOwner) { // для аннотации в метод вынесено
        assert this != unknown;
        if(writeOwner != null && this != debug && writeOwner != unknown && this != writeOwner) { // идет транзакция чужой сессии
            if(!Settings.get().isAllowNestedTransaction())
                throw new IllegalStateException("OTHER DATASESSION IN THE MIDDLE OF TRANSACTION IN THIS THREAD " + this + " " + writeOwner);

            // logged rather than asserted : it is explicitly allowed, so it should not fail with assertions enabled either
            ServerLoggers.assertLogger.info("OTHER DATASESSION IN THE MIDDLE OF TRANSACTION IN THIS THREAD " + this + " " + writeOwner + '\n' + ExecutionStackAspect.getExStackTrace());
        }
    }    
    
    public final static OperationOwner unknown = new OperationOwner() {
        public String toString() {
            return "unknown";
        }
    };

    public final static OperationOwner debug = new OperationOwner() {
        public String toString() {
            return "debug";
        }
    };

}
