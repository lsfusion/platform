package platform.client.form.tree;

import platform.client.form.ClientFormController;
import platform.client.form.GroupTree;
import platform.client.logics.ClientGroupObject;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class TreeView extends JPanel {
    private final GroupTree groupTree;

    public TreeView(ClientFormController form) {
        setLayout(new BorderLayout());

        groupTree = new GroupTree(form);

        JScrollPane pane = new JScrollPane(groupTree); 
        add(pane);
    }

    public GroupTree getTree() {
        return groupTree;
    }

    public void updateKeys(ClientGroupObject group, List<ClientGroupObjectValue> keys, List<ClientGroupObjectValue> keysTreePathes) {
        groupTree.updateKeys(group, keys, keysTreePathes);
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

    public void setCurrentSelection(ClientGroupObjectValue selectionPath) {
        groupTree.setCurrentSelection(selectionPath);
    }
}
