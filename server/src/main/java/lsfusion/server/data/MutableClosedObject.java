package lsfusion.server.data;

import lsfusion.base.MutableObject;
import lsfusion.server.ServerLoggers;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;

import java.sql.SQLException;

// local (not remote) object with SQL resources 
public abstract class MutableClosedObject<O> extends MutableObject implements AutoCloseable {

    private boolean closed;
    protected void explicitClose(O owner, boolean syncedOnClient) throws SQLException {
        ServerLoggers.assertLog(!closed, "ALREADY CLOSED " + this);
        if(syncedOnClient)
            syncedShutdown(owner, true);
        else
            shutdown(owner, true, false);
    }

    @AssertSynchronized
    // чисто для assertion'а
    protected void syncedShutdown(O owner, boolean explicit) throws SQLException {
        shutdown(owner, explicit, true);
    }

    protected boolean isClosed() {
        return closed;
    }

    protected void explicitClose() throws SQLException { // explicitClose чтобы не пересекаться с AutoClosable
        explicitClose(true);
    }

    protected void explicitClose(boolean syncedOnClient) throws SQLException {
        explicitClose(getDefaultExplicitOwner(), syncedOnClient);
    }

    @Override
    public void close() throws SQLException { // в общем случае пытается закрыть, а не закрывает объект
        explicitClose();
    }

    private void shutdown(O owner, boolean explicit, boolean syncedOnClient) throws SQLException {
        if(closed)
            return;
        if(explicit)
            onExplicitClose(owner, syncedOnClient);
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
    protected void onExplicitClose(O owner, boolean syncedOnClient) throws SQLException {
    }

    // все кроме weakRef (onExplicitClose)  !!!! ВАЖНО нельзя запускать очистку weakRef ресурсов, так как WeakReference'у уже могут стать null, и ресурсы (например временные таблицы) перейдут другому владельцу, в итоге почистятся ресурсы используемые уже новым объектом
    protected void onFinalClose(O owner) throws SQLException {
    }

    protected void finalize() throws Throwable {
        if(!ContextAwarePendingRemoteObject.disableFinalized) {
            try {
                shutdown(getFinalizeOwner(), false, false);
            } catch (SQLException e) {
            } finally {
                super.finalize();
            }
        }
    }
}
