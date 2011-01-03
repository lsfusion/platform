package skolkovo.api.remote;

import platform.interop.RemoteLogicsInterface;
import skolkovo.VoteInfo;

import java.rmi.RemoteException;

public interface SkolkovoRemoteInterface extends RemoteLogicsInterface {
    VoteInfo getVoteInfo(int expertId, int projectId) throws RemoteException;
    void setVoteInfo(int expertId, int projectId, VoteInfo voteInfo) throws RemoteException;
}
