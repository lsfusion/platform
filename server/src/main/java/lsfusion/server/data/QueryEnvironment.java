package lsfusion.server.data;

import lsfusion.server.data.type.ParseInterface;

public interface QueryEnvironment {

    ParseInterface getSQLUser();
    
    OperationOwner getOpOwner();
    
    int getTransactTimeout();

    ParseInterface getIsFullClient();
    ParseInterface getSQLComputer();
    ParseInterface getSQLForm();
    ParseInterface getIsServerRestarting();

}
