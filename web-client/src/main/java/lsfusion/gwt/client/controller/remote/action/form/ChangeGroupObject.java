package lsfusion.gwt.client.controller.remote.action.form;


import lsfusion.gwt.client.form.object.GGroupObjectValue;

public class ChangeGroupObject extends FormRequestCountingAction<ServerResponseResult> {
    public int groupId;
    public GGroupObjectValue keyValues;

    public ChangeGroupObject() {
    }

    public ChangeGroupObject(int groupId, GGroupObjectValue keyValues) {
        this.groupId = groupId;
        this.keyValues = keyValues;
    }
}
