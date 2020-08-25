package lsfusion.gwt.client.form.event;

import java.io.Serializable;

public class GBindingEnv implements Serializable {

    public Integer priority;
    public GBindingMode bindPreview;
    public GBindingMode bindDialog;
    public GBindingMode bindGroup;
    public GBindingMode bindEditing;
    public GBindingMode bindShowing;

    public static final GBindingEnv AUTO = new GBindingEnv(null, null, null, null, null, null);

    public GBindingEnv() {
    }

    public GBindingEnv(Integer priority, GBindingMode bindPreview, GBindingMode bindDialog, GBindingMode bindGroup, GBindingMode bindEditing, GBindingMode bindShowing) {
        this.priority = priority;
        this.bindPreview = bindPreview == null ? GBindingMode.ONLY : bindPreview;
        this.bindDialog = bindDialog == null ? GBindingMode.AUTO : bindDialog;
        this.bindGroup = bindGroup == null ? GBindingMode.AUTO : bindGroup;
        this.bindEditing = bindEditing == null ? GBindingMode.AUTO : bindEditing;
        this.bindShowing = bindShowing == null ? GBindingMode.AUTO : bindShowing;
    }
}
