package platform.client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;

public class ClientTreeNode<T, C extends ClientTreeNode> extends DefaultMutableTreeNode {

    public final static int NOTHING = 0;
    public final static int ONLY_NODE = 1;
    public final static int ONLY_SONS = 2;
    public final static int NODE_SONS = 3;
    public final static int SUB_TREE = 4;

    public ArrayList<Action> nodeActions = new ArrayList<Action>();
    public ArrayList<Action> sonActions = new ArrayList<Action>();
    public ArrayList<Action> subTreeActions = new ArrayList<Action>();

    public ClientTreeNode() {
        super();
    }

    public ClientTreeNode(T userObject) {
        super(userObject);
    }

    public ClientTreeNode(T userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public ClientTreeNode(T userObject, boolean allowsChildren, int mode, Action... actions) {
        super(userObject, allowsChildren);
        setActions(mode, actions);
    }

    public T getTypedObject() {
        return (T) super.getUserObject();
    }

    public C getSiblingNode(TransferHandler.TransferSupport info) {

        ClientTreeNode treeNode = ClientTree.getNode(info);
        if (treeNode == null || getClass() != treeNode.getClass()) return null;
        if (getParent() != treeNode.getParent()) return null;

        return (C)treeNode;
    }

    public void setActions(int mode, Action... actions) {
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

    public void addNodeAction(Action... actions) {
        for (Action act : actions) {
            nodeActions.add(act);
        }
    }

    public void addSonAction(Action... actions) {
        for (Action act : actions) {
            sonActions.add(act);
        }
    }

    public void addSubTreeAction(Action... actions) {
        for (Action act : actions) {
            subTreeActions.add(act);
        }
    }

    public boolean canImport(TransferHandler.TransferSupport info){
        return false;
    }

    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info){
        return false;
    }

    public void exportDone(JComponent component, int mode){
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

        return thisObj != null && otherObj != null && thisObj.equals(otherObj);
    }
}
