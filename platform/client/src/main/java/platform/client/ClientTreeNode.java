package platform.client;

import platform.base.BaseUtils;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.nodes.NodeCreator;
import platform.client.descriptor.nodes.NullFieldNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

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
        if (treeNode == null || getClass() != treeNode.getClass()) {
            return null;
        }
        if (getParent() != treeNode.getParent()) {
            return null;
        }

        return (C) treeNode;
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

    protected void addFieldReferenceNode(Object object, String field, String fieldCaption, Object context, String[] derivedNames, Class[] derivedClasses) {
        Object value = BaseUtils.invokeGetter(object, field);
        ClientTreeNode newNode;
        if (value == null) {
            newNode = new NullFieldNode(fieldCaption);
        } else if (value instanceof NodeCreator) {
            newNode = ((NodeCreator) value).createNode(context);
        } else {
            return;
        }

        newNode.addInitializeReferenceActions(object, field, derivedNames, derivedClasses);
        add(newNode);
    }

    public void addInitializeReferenceActions(final Object object, final String field, String[] captions, final Class[] classes) {
        addNodeAction(new AbstractAction("Обнулить") {
            public void actionPerformed(ActionEvent e) {
                BaseUtils.invokeSetter(object, field, null);
            }
        });

        for (int i = 0; i < captions.length; i++) {
            final int prm = i;
            addNodeAction(new AbstractAction("Инициализировать как " + captions[i]) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        BaseUtils.invokeSetter(object, field, classes[prm].newInstance());
                    } catch (InstantiationException e1) {
                        throw new RuntimeException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
        }
    }

    public void addCollectionReferenceActions(final Object object, final String collectionField, String[] captions, final Class[] classes) {
        for (int i = 0; i < captions.length; i++) {
            final int prm = i;
            addNodeAction(new AbstractAction("Добавить " + captions[i]) {
                public void actionPerformed(ActionEvent e) {
                    try {
                        Object val = BaseUtils.invokeGetter(object, collectionField);
                        if (val instanceof Collection) {
                            Collection collection = (Collection) val;
                            collection.add(classes[prm].newInstance());
                            IncrementDependency.update(object, collectionField);
                        }
                    } catch (InstantiationException e1) {
                        throw new RuntimeException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
        }
    }

    public boolean canImport(TransferHandler.TransferSupport info) {
        return false;
    }

    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return false;
    }

    public void exportDone(JComponent component, int mode) {
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
