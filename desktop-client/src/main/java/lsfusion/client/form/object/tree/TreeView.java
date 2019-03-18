package lsfusion.client.form.object.tree;

import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.layout.view.JComponentPanel;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.ClientTreeGroup;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class TreeView extends JComponentPanel {
    private final TreeGroupTable groupTree;

    public TreeView(ClientFormController form, ClientTreeGroup treeGroup) {
        groupTree = new TreeGroupTable(form, treeGroup);

        treeGroup.installMargins(this);

        add(new JScrollPane(groupTree));
    }

    @Override
    public Dimension getMaxPreferredSize() { // ради этого вся ветка maxPreferredSize и делалась
        return groupTree.getMaxPreferredSize(getPreferredSize());
    }

    public TreeGroupTable getTree() {
        return groupTree;
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> keysTreePathes, Map<ClientGroupObjectValue, Boolean> expandables) {
        groupTree.updateKeys(group, keys, keysTreePathes, expandables);
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
