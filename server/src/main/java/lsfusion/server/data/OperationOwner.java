package lsfusion.server.data;

import lsfusion.server.ServerLoggers;

public abstract class OperationOwner {
    
    @AssertSynchronized
    public void checkThreadSafeAccess(OperationOwner writeOwner) { // для аннотации в метод вынесено
        if(writeOwner != null && this != debug) // идет транзакция
            ServerLoggers.assertLog(this == writeOwner, "OTHER DATASESSION IN THE MIDDLE OF TRANSACTION IN THIS THREAD " + this + " " + writeOwner);
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
