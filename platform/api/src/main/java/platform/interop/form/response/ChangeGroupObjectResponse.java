package platform.interop.form.response;

import java.io.Serializable;

public class ChangeGroupObjectResponse implements Serializable {

    public byte[] formChanges;
    public int classID;

    public ChangeGroupObjectResponse(byte[] formChanges, int classID) {
        this.formChanges = formChanges;
        this.classID = classID;
    }
}
