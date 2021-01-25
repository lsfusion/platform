package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.classes.GType;

public class GAsyncChange extends GAsyncExec {
    public GType changeType;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncChange() {
    }

    public GAsyncChange(GType changeType) {
        this.changeType = changeType;
    }
}
