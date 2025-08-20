package lsfusion.client.navigator.tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collections;

public class ClientTreeNode<T> extends DefaultMutableTreeNode {

    public final static int ONLY_NODE = 1;
    public final static int ONLY_SONS = 2;
    public final static int NODE_SONS = 3;
    public final static int SUB_TREE = 4;

    public ArrayList<ClientTreeAction> nodeActions = new ArrayList<>();
    public ArrayList<ClientTreeAction> sonActions = new ArrayList<>();
    public ArrayList<ClientTreeAction> subTreeActions = new ArrayList<>();

    public ClientTreeNode() {
        super();
    }

    public ClientTreeNode(T userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public ClientTreeNode(T userObject, boolean allowsChildren, int mode, ClientTreeAction... actions) {
        super(userObject, allowsChildren);
        setActions(mode, actions);
    }

    public void setActions(int mode, ClientTreeAction... actions) {
        if (mode == ONLY_NODE || mode == NODE_SONS) {
            nodeActions.clear();
            addNodeAction(actions);
        }
        if (mode == ONLY_SONS || mode == NODE_SONS) {
            sonActions.clear();
            addSonAction(actions);
        }
        if (mode == SUB_TREE) {
            subTreeActions.clear();
            addSubTreeAction(actions);
        }
    }

    public void addNodeAction(ClientTreeAction... actions) {
        Collections.addAll(nodeActions, actions);
    }

    public void addSonAction(ClientTreeAction... actions) {
        Collections.addAll(sonActions, actions);
    }

    public void addSubTreeAction(ClientTreeAction... actions) {
        Collections.addAll(subTreeActions, actions);
    }

    public boolean canImport() {
        return false;
    }

    public boolean importData() {
        return false;
    }

    public void exportDone() {
    }

    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ClientTreeNode)) {
            return false;
        }

        ClientTreeNode otherNode = (ClientTreeNode) obj;

        Object thisObj = getUserObject();
        Object otherObj = otherNode.getUserObject();

        return thisObj != null && thisObj.equals(otherObj);
    }
}
