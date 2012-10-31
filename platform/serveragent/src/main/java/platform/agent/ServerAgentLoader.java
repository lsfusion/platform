package platform.agent;

import platform.interop.RemoteServerAgentLoaderInterface;

import java.io.IOException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerAgentLoader extends UnicastRemoteObject implements RemoteServerAgentLoaderInterface {
    private List<String> dbNames = new ArrayList<String>();

    public ServerAgentLoader() throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        super();
    }

    public void setDbName(String dbName) {
        if(!dbNames.contains(dbName))
            dbNames.add(dbName);
    }
    
    public List<String> getDbNames(){
        return dbNames;
    }
}
