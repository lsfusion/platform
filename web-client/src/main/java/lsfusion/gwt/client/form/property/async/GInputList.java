package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.cell.classes.controller.suggest.GCompletionType;

import java.io.Serializable;

public class GInputList implements Serializable {
    public GCompletionType completionType;
    public GCompare compare;

    public GInputList() {
    }

    public GInputList(GCompletionType completionType, GCompare compare) {
        this.completionType = completionType;
        this.compare = compare;
    }
}
