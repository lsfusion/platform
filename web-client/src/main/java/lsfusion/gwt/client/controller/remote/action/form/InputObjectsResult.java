package lsfusion.gwt.client.controller.remote.action.form;

import net.customware.gwt.dispatch.shared.Result;

import java.util.List;

public class InputObjectsResult implements Result {
    public List<String> inputObjects;

    public InputObjectsResult() {
    }

    public InputObjectsResult(List<String> inputObjects) {
        this.inputObjects = inputObjects;
    }
}