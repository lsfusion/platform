package lsfusion.gwt.shared.result;

import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;

public class ListResult implements Result {
    public ArrayList value;

    @SuppressWarnings("UnusedDeclaration")
    public ListResult() {
    }

    public ListResult(ArrayList value) {
        this.value = value;
    }
}