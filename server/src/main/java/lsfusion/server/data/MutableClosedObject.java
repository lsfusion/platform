package lsfusion.server.data;

import lsfusion.base.MutableObject;
import lsfusion.server.ServerLoggers;

import java.sql.SQLException;

// local (not remote) object with SQL resources 
public abstract class MutableClosedObject<O> extends MutableObject implements AutoCloseable {

    private boolean closed;
    @AssertSynchronized
    protected void explicitClose(O owner) throws SQLException {
        ServerLoggers.assertLog(!closed, "ALREADY CLOSED " + this);
        shutdown(owner, true);
    }
    
    protected boolean isClosed() {
        return closed;
    }

    protected void explicitClose() throws SQLException { // explicitClose чтобы не пересекаться с AutoClosable
        explicitClose(getDefaultExplicitOwner());
    }

    @Override
    public void close() throws SQLException { // не использовать напряму
        explicitClose();
    }

    private void shutdown(O owner, boolean explicit) throws SQLException {
        if(closed)
            return;
        if(explicit)
            onExplicitClose(owner);
        onFinalClose(owner);
        closed = true;
    }

    public O getDefaultExplicitOwner() {
        return null;
    }


    public O getFinalizeOwner() {
        return null;
    }
    
    // явная очистка ресурсов, которые поддерживаются через weak ref'ы
    protected void onExplicitClose(O owner) throws SQLException {
    }

    // все кроме weakRef (onExplicitClose)  !!!! ВАЖНО нельзя запускать очистку weakRef ресурсов, так как WeakReference'у уже могут стать null, и ресурсы (например временные таблицы) перейдут другому владельцу, в итоге почистятся ресурсы используемые уже новым объектом
    protected void onFinalClose(O owner) throws SQLException {
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
