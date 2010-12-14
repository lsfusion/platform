package platform.server.data;

import platform.server.data.type.TypeObject;

public interface StatementParams {

    TypeObject getSQLUser();
    TypeObject getID();
    TypeObject getSQLComputer();

}
