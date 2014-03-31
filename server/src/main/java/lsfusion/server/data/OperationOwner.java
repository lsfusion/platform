package lsfusion.server.data;

public interface OperationOwner {
    
    public final static OperationOwner unknown = new OperationOwner() {
    };

    public final static OperationOwner debug = new OperationOwner() {
    };

}
