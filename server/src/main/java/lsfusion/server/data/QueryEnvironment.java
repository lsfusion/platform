package lsfusion.server.data;

import lsfusion.server.data.type.parse.ValueParseInterface;

import java.util.Locale;

public interface QueryEnvironment {

    OperationOwner getOpOwner();
    
    int getTransactTimeout();

    ValueParseInterface getSQLUser();
    ValueParseInterface getSQLComputer();
    ValueParseInterface getSQLForm();
    ValueParseInterface getSQLConnection();
    ValueParseInterface getIsServerRestarting();
    ValueParseInterface getSQLAuthToken();

    Locale getLocale();
}
