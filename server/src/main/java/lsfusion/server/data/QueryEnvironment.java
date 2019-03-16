package lsfusion.server.data;

import lsfusion.server.data.type.parse.ParseInterface;

import java.util.Locale;

public interface QueryEnvironment {

    OperationOwner getOpOwner();
    
    int getTransactTimeout();

    ParseInterface getSQLUser();
    ParseInterface getSQLComputer();
    ParseInterface getSQLForm();
    ParseInterface getSQLConnection();
    ParseInterface getIsServerRestarting();

    ParseInterface getSQLAuthToken();

    Locale getLocale();
}
