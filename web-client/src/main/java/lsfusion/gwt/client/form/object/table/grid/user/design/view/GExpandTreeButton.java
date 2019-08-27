package lsfusion.gwt.client.form.object.table.grid.user.design.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.tree.controller.GTreeGroupController;

public class GExpandTreeButton extends GToolbarButton {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final GTreeGroupController treeGroupController;
    private final boolean current;

    public GExpandTreeButton(GTreeGroupController treeGroupController, boolean current) {
        super(current ? "expandTreeCurrent.png" : "expandTree.png", "");
        this.treeGroupController = treeGroupController;
        this.current = current;
        setTitle(current ? messages.formTreeExpandCurrent() : messages.formTreeExpand());
    }

    @Override
    public void addListener() {
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                treeGroupController.fireExpandNodeRecursive(current);
            }
        });
    }
}