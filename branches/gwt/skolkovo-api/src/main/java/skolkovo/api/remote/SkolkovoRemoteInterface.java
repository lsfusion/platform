package skolkovo.api.remote;

import platform.interop.RemoteLogicsInterface;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.gwt.shared.VoteInfo;

import java.rmi.RemoteException;

public interface SkolkovoRemoteInterface extends RemoteLogicsInterface {
    VoteInfo getVoteInfo(String voteId) throws RemoteException;
    void setVoteInfo(String voteId, VoteInfo voteInfo) throws RemoteException;
    ProfileInfo getProfileInfo(String expertLogin) throws RemoteException;
    void sentVoteDocuments(String login, int voteId) throws RemoteException;
    void remindPassword(String email) throws RemoteException;
}
