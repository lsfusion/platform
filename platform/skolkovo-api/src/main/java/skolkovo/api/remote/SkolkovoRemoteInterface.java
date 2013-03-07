package skolkovo.api.remote;

import platform.interop.RemoteLogicsInterface;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.api.gwt.shared.VoteInfo;

import java.rmi.RemoteException;

public interface SkolkovoRemoteInterface extends RemoteLogicsInterface {
    VoteInfo getVoteInfo(String voteId, String locale) throws RemoteException;
    void setVoteInfo(String voteId, VoteInfo voteInfo) throws RemoteException;
    ProfileInfo getProfileInfo(String expertLogin, String locale) throws RemoteException;
    void setProfileInfo(String expertLogin, ProfileInfo profileInfo) throws RemoteException;
    void sentVoteDocuments(String login, int voteId) throws RemoteException;
    void setConfResult(String conferenceHash, boolean result) throws RemoteException;
}
