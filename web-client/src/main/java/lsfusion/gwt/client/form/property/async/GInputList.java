package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;

import java.io.Serializable;

public class GInputList implements Serializable {

    public GInputListAction[] actions; // null if there is no list
    public GCompletionType completionType;

    public GInputList() {
    }

    public GInputList(GInputListAction[] actions, GCompletionType completionType) {
        this.actions = actions;
        this.completionType = completionType;
    }
}
