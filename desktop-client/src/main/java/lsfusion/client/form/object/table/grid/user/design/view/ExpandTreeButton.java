package lsfusion.client.form.object.table.grid.user.design.view;

import lsfusion.client.form.filter.user.FilterView;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.object.table.tree.controller.TreeGroupController;

import javax.swing.*;

import static lsfusion.client.ClientResourceBundle.getString;

public class ExpandTreeButton extends ToolbarGridButton {
    public static final ImageIcon EXPAND_TREE_ICON = new ImageIcon(FilterView.class.getResource("/images/expandTree.png"));
    public static final ImageIcon EXPAND_TREE_CURRENT_ICON = new ImageIcon(FilterView.class.getResource("/images/expandTreeCurrent.png"));

    public ExpandTreeButton(TreeGroupController treeGroupController, boolean current) {
        super(current ? EXPAND_TREE_CURRENT_ICON : EXPAND_TREE_ICON, null);
        setToolTipText(current ? getString("form.tree.expand.current") : getString("form.tree.expand"));
        addActionListener(e -> {
            treeGroupController.getFormController().expandGroupObjectRecursive(treeGroupController.getCurrentGroupObject(), current);
        });
    }
}