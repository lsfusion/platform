package platform.interop.form;

import platform.interop.action.ClientAction;

import java.io.Serializable;
import java.util.List;

public class RemoteChanges implements Serializable {

    public final byte[] form;
    public final List<ClientAction> actions;
    public final int classID;

    public final boolean continueInteraction;

    public RemoteChanges(byte[] form, List<ClientAction> actions, int classID, boolean continueInteraction) {
        this.form = form;
        this.actions = actions;
        this.classID = classID;
        this.continueInteraction = continueInteraction;
    }
}
