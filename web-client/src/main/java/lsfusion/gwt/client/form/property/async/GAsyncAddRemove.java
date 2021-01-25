package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.object.GObject;

public class GAsyncAddRemove extends GAsyncExec{
    public GObject object;
    public Boolean add;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncAddRemove() {
    }

    public GAsyncAddRemove(GObject object, Boolean add) {
        this.object = object;
        this.add = add;
    }
}
