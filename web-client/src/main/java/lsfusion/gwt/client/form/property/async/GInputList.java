package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;

import java.io.Serializable;

public class GInputList implements Serializable {

    public String[] actions; // null if there is no list
    public GAsyncExec[] actionAsyncs;
    public GCompletionType completionType;
    public boolean[] quickAccesses;

    public GInputList() {
    }

    public GInputList(String[] actions, GAsyncExec[] actionAsyncs, GCompletionType completionType) {
        this.actions = actions;
        this.actionAsyncs = actionAsyncs;
        this.completionType = completionType;
    }
}
