package platform.client.form.tree;

import platform.client.form.ClientFormController;
import platform.client.form.GroupTree;
import platform.client.form.TreeGroupController;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class TreeView extends JPanel {
    private final GroupTree groupTree;

    public TreeView(TreeGroupController treeGroupController, ClientFormController form) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        groupTree = new GroupTree(treeGroupController, form);
    }

    public GroupTree getTree() {
        return groupTree;
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> keysTreePathes, boolean refreshKeys) {
        groupTree.updateKeys(group, keys, keysTreePathes, refreshKeys);
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        groupTree.updateDrawPropertyValues(property, values);
    }

    public void addDrawProperty(ClientPropertyDraw property) {
        groupTree.addDrawProperty(property);
    }

    public void removeProperty(ClientPropertyDraw property) {
        groupTree.removeProperty(property);
    }
}
