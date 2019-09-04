package lsfusion.client.form.object.table.grid.user.design.view;

import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.object.table.tree.controller.TreeGroupController;

import javax.swing.*;

import static lsfusion.client.ClientResourceBundle.getString;

public class ExpandTreeButton extends ToolbarGridButton {
    public static final ImageIcon EXPAND_TREE_ICON = new ImageIcon(FilterView.class.getResource("/images/expandTree.png"));
    public static final ImageIcon COLLAPSE_TREE_ICON = new ImageIcon(FilterView.class.getResource("/images/collapseTree.png"));
    public static final ImageIcon EXPAND_TREE_CURRENT_ICON = new ImageIcon(FilterView.class.getResource("/images/expandTreeCurrent.png"));
    public static final ImageIcon COLLAPSE_TREE_CURRENT_ICON = new ImageIcon(FilterView.class.getResource("/images/collapseTreeCurrent.png"));

    private final boolean current;
    private boolean expand;

    public ExpandTreeButton(TreeGroupController treeGroupController, boolean current) {
        super(current ? EXPAND_TREE_CURRENT_ICON : EXPAND_TREE_ICON, null);
        this.current = current;
        update(treeGroupController);
        addActionListener(e -> {
            if (expand) {
                treeGroupController.getFormController().expandGroupObjectRecursive(treeGroupController.getCurrentGroupObject(), current);
            } else {
                treeGroupController.getFormController().collapseGroupObjectRecursive(treeGroupController.getCurrentGroupObject(), current);
            }
        });
    }

    public void update(TreeGroupController treeGroupController) {
        this.expand = !treeGroupController.isCurrentPathExpanded();
        setIcon(current ? (expand ? EXPAND_TREE_CURRENT_ICON : COLLAPSE_TREE_CURRENT_ICON) : (expand ? EXPAND_TREE_ICON : COLLAPSE_TREE_ICON));
        updateToolTipText();
    }

    private void updateToolTipText() {
        setToolTipText(current ? (expand ? getString("form.tree.expand.current") : getString("form.tree.collapse.current")) : (expand ? getString("form.tree.expand") : getString("form.tree.collapse")));
    }
}