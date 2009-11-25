package platform.server.session;

import platform.server.data.type.Type;

import java.sql.SQLException;

public interface DataChange {

    Type getType();

    public void change(DataSession session, TableModifier<? extends TableChanges> modifier, Object newValue, boolean externalID) throws SQLException;
}
