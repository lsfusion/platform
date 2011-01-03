package skolkovo.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import platform.interop.RemoteLoaderInterface;
import skolkovo.gwt.client.ExpertService;
import skolkovo.api.remote.SkolkovoRemoteInterface;

import java.rmi.Naming;
import java.rmi.RemoteException;

public class ExpertServiceImpl extends RemoteServiceServlet implements ExpertService {
    private static SkolkovoRemoteInterface getBL() {
        System.out.println("getting bl...");
        try {
            RemoteLoaderInterface loader = (RemoteLoaderInterface) Naming.lookup("rmi://" + "localhost" + ":" + "7652" + "/BusinessLogics");
            return (SkolkovoRemoteInterface) loader.getRemoteLogics();
        } catch (Exception e) {
            System.err.println("bl is null...");
            e.printStackTrace();
            return null;
        }
    }

    public String[] getProjects() {
        String[] result = null;
        try {
            SkolkovoRemoteInterface bl = getBL();
            if (bl != null) {
                result = getBL().getProjectNames(0);
            }
        } catch (RemoteException e) {
            System.err.println("Exception while getting...");
            result = null;
        }

        return result != null
               ? result
               : new String[]{"aurora1", "gannimed1", "mustang1"};
    }
}