package lsfusion.server.data;

import lsfusion.base.MutableObject;
import lsfusion.server.ServerLoggers;

import java.sql.SQLException;

// local (not remote) object with SQL resources 
public abstract class MutableClosedObject<O> extends MutableObject implements AutoCloseable {

    private boolean closed;

    protected boolean isClosed() {
        return closed;
    }

    public void close(boolean syncedOnClient) throws SQLException {
        ServerLoggers.assertLog(!closed, "ALREADY CLOSED " + this);

        if(closed)
            return;

        onClose(getDefaultCloseOwner(), syncedOnClient);

        closed = true;
    }

    @Override
    public void close() throws SQLException { // в общем случае пытается закрыть, а не закрывает объект
        close(true);
    }

    public O getDefaultCloseOwner() {
        return null;
    }


    // явная очистка ресурсов, которые поддерживаются через weak ref'ы
    protected void onClose(O owner, boolean syncedOnClient) throws SQLException {
    }
}
