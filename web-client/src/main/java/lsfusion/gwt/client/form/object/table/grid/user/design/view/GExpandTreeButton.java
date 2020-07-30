package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;

public class GExpandTreeButton extends GToolbarButton {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final GTreeGroupController treeGroupController;
    private final boolean current;
    private boolean expand;

    public GExpandTreeButton(GTreeGroupController treeGroupController, boolean current) {
        super(current ? "expandTreeCurrent.png" : "expandTree.png", "");
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

    public void update(GTreeGroupController treeGroupController) {
        this.expand = !treeGroupController.isCurrentPathExpanded();
        setModuleImagePath(current ? (expand ? "expandTreeCurrent.png" : "collapseTreeCurrent.png") : (expand ? "expandTree.png" : "collapseTree.png"));
        updateToolTipText();
    }

    private void updateToolTipText() {
        setTitle(current ? (expand ? messages.formTreeExpandCurrent() : messages.formTreeCollapseCurrent()) : (expand ? messages.formTreeExpand() : messages.formTreeCollapse()));
    }
}