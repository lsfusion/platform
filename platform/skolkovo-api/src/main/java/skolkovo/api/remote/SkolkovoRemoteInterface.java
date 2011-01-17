package skolkovo.api.remote;

import platform.interop.RemoteLogicsInterface;
import skolkovo.VoteInfo;

import java.rmi.RemoteException;

public interface SkolkovoRemoteInterface extends RemoteLogicsInterface {
    VoteInfo getVoteInfo(String voteId) throws RemoteException;
    void setVoteInfo(String voteId, VoteInfo voteInfo) throws RemoteException;
}
