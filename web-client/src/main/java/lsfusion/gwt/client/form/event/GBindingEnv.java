package lsfusion.gwt.client.form.event;

import java.io.Serializable;

public class GBindingEnv implements Serializable {

    public Integer priority;
    public GBindingMode bindPreview;
    public GBindingMode bindDialog;
    public GBindingMode bindWindow;
    public GBindingMode bindGroup;
    public GBindingMode bindEditing;
    public GBindingMode bindShowing;
    public GBindingMode bindPanel;
    public GBindingMode bindCell;

    public GBindingEnv() {
    }

    public GBindingEnv(Integer priority, GBindingMode bindPreview, GBindingMode bindDialog, GBindingMode bindWindow, GBindingMode bindGroup,
                       GBindingMode bindEditing, GBindingMode bindShowing, GBindingMode bindPanel, GBindingMode bindCell) {
        this.priority = priority;
        this.bindPreview = bindPreview == null ? GBindingMode.AUTO : bindPreview;
        this.bindDialog = bindDialog == null ? GBindingMode.AUTO : bindDialog;
        this.bindWindow = bindWindow == null ? GBindingMode.AUTO : bindWindow;
        this.bindGroup = bindGroup == null ? GBindingMode.AUTO : bindGroup;
        this.bindEditing = bindEditing == null ? GBindingMode.AUTO : bindEditing;
        this.bindShowing = bindShowing == null ? GBindingMode.AUTO : bindShowing;
        this.bindPanel = bindPanel == null ? GBindingMode.AUTO : bindPanel;
        this.bindCell = bindCell == null ? GBindingMode.AUTO : bindCell;
    }
}
