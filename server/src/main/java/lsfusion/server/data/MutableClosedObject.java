package lsfusion.base;

import lsfusion.server.ServerLoggers;
import lsfusion.server.data.AssertSynchronized;

import java.sql.SQLException;

// local (not remote) object with SQL resources 
public abstract class MutableClosedObject<O> extends MutableObject {

    private boolean closed;
    @AssertSynchronized
    public void close(O owner) throws SQLException {
        ServerLoggers.assertLog(!closed, "ALREADY CLOSED");
        shutdown(owner, true);
    }
    
    protected boolean isClosed() {
        return closed;
    }
    
    public void close() throws SQLException {
        close(null);
    }

    private void shutdown(O owner, boolean explicit) throws SQLException {
        if(closed)
            return;
        if(explicit)
            explicitClose(owner);
        finalClose(owner);
        closed = true;
    }
    
    public O getFinalizeOwner() {
        return null;
    }
    
    // явная очистка ресурсов, которые также поддерживаются через weak ref'ы
    // это нельзя делать в finalClose так как нарушит много assertion'ов
    protected void explicitClose(O owner) throws SQLException {
    }

    // explicit resource cleaning which is not implemented with weak refs  
    protected void finalClose(O owner) throws SQLException {
    }

    protected void finalize() throws Throwable {
        try
        {
            shutdown(getFinalizeOwner(), false);
        }
        catch (SQLException e)
        {
        }
        finally
        {
            super.finalize();
        }
    }
}
