package platform.client.form.tree;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.ClientTreeGroup;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class TreeView extends JPanel {
    private final TreeGroupTable groupTree;

    public TreeView(ClientFormController form, ClientTreeGroup treeGroup) {
        setLayout(new BorderLayout());

        groupTree = new TreeGroupTable(form, treeGroup);

        add(new JScrollPane(groupTree));
    }

    public TreeGroupTable getTree() {
        return groupTree;
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> keysTreePathes) {
        groupTree.updateKeys(group, keys, keysTreePathes);
    }

    public void updateDrawPropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values, boolean update) {
        groupTree.updateDrawPropertyValues(property, values, update);
    }

    public void addDrawProperty(ClientGroupObject group, ClientPropertyDraw property, boolean toPanel) {
        groupTree.addDrawProperty(group, property);
    }

    public void removeProperty(ClientGroupObject group, ClientPropertyDraw property) {
        groupTree.removeProperty(group, property);
    }

    public void setCurrentPath(ClientGroupObjectValue objects) {
        groupTree.setCurrentPath(objects);
    }

    public ClientGroupObjectValue getCurrentPath() {
        return groupTree.getCurrentPath();
    }
}
