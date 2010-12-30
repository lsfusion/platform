package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import platform.interop.RemoteLoaderInterface;
import platform.interop.RemoteLogicsInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

public class BusinessLogicsLoader extends UnicastRemoteObject implements RemoteLoaderInterface {
    private final BusinessLogics BL;

    public BusinessLogicsLoader(BusinessLogics BL) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(BL.getExportPort());
        this.BL = BL;
    }

    public RemoteLogicsInterface getRemoteLogics() throws RemoteException {
        return BL;
    }

    public byte[] findClass(String name) throws RemoteException {
        return  BL.findClass(name);
    }
}
