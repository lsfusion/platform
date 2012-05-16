package main.java;

import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServerAgentLoader extends UnicastRemoteObject implements RemoteLoaderInterface {
    private List<String> dbNames = new ArrayList<String>();

    public ServerAgentLoader() throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        super();
    }

    public RemoteLogicsInterface getRemoteLogics() throws RemoteException {
        return null;
    }
    
    public void setDbName(String dbName) {
        if(!dbNames.contains(dbName))
            dbNames.add(dbName);
    }
    
    public List<String> getDbNames(){
        return dbNames;
    }

    public byte[] findClass(String name) throws RemoteException {
        return null;
    }
}
