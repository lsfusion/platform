package lsfusion.gwt.client.form.property.async;

import java.io.Serializable;

public class GInputList implements Serializable {

    public String[] actions; // null if there is no list
    public GAsyncExec[] actionAsyncs;
    public boolean strict;

    public GInputList() {
    }

    public GInputList(String[] actions, GAsyncExec[] actionAsyncs, boolean strict) {
        this.actions = actions;
        this.actionAsyncs = actionAsyncs;
        this.strict = strict;
    }
}
