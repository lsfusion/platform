package skolkovo.api.remote;

import platform.interop.RemoteLogicsInterface;
import skolkovo.VoteInfo;

import java.rmi.RemoteException;

public interface SkolkovoRemoteInterface extends RemoteLogicsInterface {
    VoteInfo getVoteInfo(String login, int voteId) throws RemoteException;
    void setVoteInfo(int expertId, int voteId, VoteInfo voteInfo) throws RemoteException;
}
