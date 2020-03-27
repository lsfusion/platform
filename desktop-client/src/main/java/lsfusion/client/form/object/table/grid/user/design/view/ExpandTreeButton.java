package lsfusion.client.form.object.table.grid.user.design.view;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.object.table.tree.controller.TreeGroupController;

import static lsfusion.client.ClientResourceBundle.getString;

public class ExpandTreeButton extends ToolbarGridButton {
    public static final String EXPAND_TREE_ICON_PATH = "expandTree.png";
    public static final String COLLAPSE_TREE_ICON_PATH = "collapseTree.png";
    public static final String EXPAND_TREE_CURRENT_ICON_PATH = "expandTreeCurrent.png";
    public static final String COLLAPSE_TREE_CURRENT_ICON_PATH = "collapseTreeCurrent.png";

    private final boolean current;
    private boolean expand;

    public ExpandTreeButton(TreeGroupController treeGroupController, boolean current) {
        super(current ? EXPAND_TREE_CURRENT_ICON_PATH : EXPAND_TREE_ICON_PATH, null);
        this.current = current;
        update(treeGroupController);
        addActionListener(e -> {
            ClientGroupObject currentGroupObject = treeGroupController.getCurrentGroupObject();
            if(currentGroupObject != null) {
                if (expand) {
                    treeGroupController.getFormController().expandGroupObjectRecursive(currentGroupObject, current);
                } else {
                    treeGroupController.getFormController().collapseGroupObjectRecursive(currentGroupObject, current);
                }
            }
        });
    }

    public void update(TreeGroupController treeGroupController) {
        this.expand = !treeGroupController.isCurrentPathExpanded();
        setIconPath(current ? (expand ? EXPAND_TREE_CURRENT_ICON_PATH : COLLAPSE_TREE_CURRENT_ICON_PATH) : (expand ? EXPAND_TREE_ICON_PATH : COLLAPSE_TREE_ICON_PATH));
        updateToolTipText();
    }

    private void updateToolTipText() {
        setToolTipText(current ? (expand ? getString("form.tree.expand.current") : getString("form.tree.collapse.current")) : (expand ? getString("form.tree.expand") : getString("form.tree.collapse")));
    }
}