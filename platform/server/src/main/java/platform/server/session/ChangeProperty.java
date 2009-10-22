package platform.server.session;

import platform.server.data.types.Type;

import java.sql.SQLException;

public interface ChangeProperty {

    Type getType();

    public void change(ChangesSession session, Object newValue, boolean externalID) throws SQLException;    
}
