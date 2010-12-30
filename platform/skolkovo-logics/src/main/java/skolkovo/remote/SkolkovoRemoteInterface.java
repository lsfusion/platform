package skolkovo.remote;

import platform.interop.RemoteLogicsInterface;

public interface SkolkovoRemoteInterface extends RemoteLogicsInterface {
    String[] getProjectNames(int expertId);
}
