package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.tree.controller.GPlatformTreeController;

public class GExpandTreeButton extends GToolbarButton {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final GPlatformTreeController treeGroupController;
    private final boolean current;
    private boolean expand;

    public GExpandTreeButton(GPlatformTreeController treeGroupController, boolean current) {
        super(current ? StaticImage.EXPANDTREECURRENT : StaticImage.EXPANDTREE, "");
        this.treeGroupController = treeGroupController;
        this.current = current;
        updateToolTipText();
    }

    @Override
    public ClickHandler getClickHandler() {
        return event -> {
            if(expand) {
                treeGroupController.fireExpandNodeRecursive(current);
            } else {
                treeGroupController.fireCollapseNodeRecursive(current);
            }
            update(treeGroupController);
        };
    }

    public void update(GPlatformTreeController treeGroupController) {
        this.expand = true;//!treeGroupController.isCurrentPathExpanded();
        changeImage(current ? (expand ? StaticImage.EXPANDTREECURRENT : StaticImage.COLLAPSETREECURRENT) : (expand ? StaticImage.EXPANDTREE : StaticImage.COLLAPSETREE));
        updateToolTipText();
    }

    private void updateToolTipText() {
        setTitle(current ? (expand ? messages.formTreeExpandCurrent() : messages.formTreeCollapseCurrent()) : (expand ? messages.formTreeExpand() : messages.formTreeCollapse()));
    }
}