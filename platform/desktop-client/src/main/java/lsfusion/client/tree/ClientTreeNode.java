package lsfusion.client.tree;

import lsfusion.base.BaseUtils;
import lsfusion.base.context.ApplicationContextHolder;
import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.base.context.Lookup;
import lsfusion.base.identity.IdentityInterface;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.CustomConstructible;
import lsfusion.client.descriptor.nodes.NodeCreator;
import lsfusion.client.descriptor.nodes.NullFieldNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;

public class ClientTreeNode<T, C extends ClientTreeNode> extends DefaultMutableTreeNode {

    public final static int NOTHING = 0;
    public final static int ONLY_NODE = 1;
    public final static int ONLY_SONS = 2;
    public final static int NODE_SONS = 3;
    public final static int SUB_TREE = 4;

    public ArrayList<ClientTreeAction> nodeActions = new ArrayList<ClientTreeAction>();
    public ArrayList<ClientTreeAction> sonActions = new ArrayList<ClientTreeAction>();
    public ArrayList<ClientTreeAction> subTreeActions = new ArrayList<ClientTreeAction>();

    public ClientTreeNode() {
        super();
    }

    public ClientTreeNode(T userObject) {
        super(userObject);
    }

    public ClientTreeNode(T userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public ClientTreeNode(T userObject, boolean allowsChildren, int mode, ClientTreeAction... actions) {
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
        addNodeAction(new ClientTreeAction(ClientResourceBundle.getString("tree.reset")) {
            public void actionPerformed(ClientTreeActionEvent e) {
                BaseUtils.invokeSetter(object, field, null);
            }
        });

        for (int i = 0; i < captions.length; i++) {
            final int prm = i;
            addNodeAction(new ClientTreeAction(ClientResourceBundle.getString("tree.initialize.as")+" " + captions[i]) {
                public void actionPerformed(ClientTreeActionEvent e) {
                    try {
                        BaseUtils.invokeSetter(object, field, processCreatedObject(classes[prm].newInstance(), object));
                    } catch (InstantiationException e1) {
                        throw new RuntimeException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
        }
    }

    public void addCollectionReferenceActions(final Object object, final String collectionField, final String[] captions, final Class[] classes) {
        for (int i = 0; i < captions.length; i++) {
            final int prm = i;
            addNodeAction(new ClientTreeAction(ClientResourceBundle.getString("tree.add") + (captions.length > 1 ? " " + captions[i] : "")) {
                public void actionPerformed(ClientTreeActionEvent e) {
                    try {
                        BaseUtils.invokeAdder(object, collectionField, processCreatedObject(classes[prm].newInstance(), object));
                    } catch (InstantiationException e1) {
                        throw new RuntimeException(e1);
                    } catch (IllegalAccessException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
        }

        addSonAction(new ClientTreeAction(ClientResourceBundle.getString("tree.delete"), KeyEvent.VK_DELETE) {
            @Override
            public void actionPerformed(ClientTreeActionEvent e) {
                Object deletedObject = e.getNode().getTypedObject();
                BaseUtils.invokeRemover(object, collectionField, deletedObject);
                if (object instanceof ApplicationContextProvider) {
                    ApplicationContextProvider provider = (ApplicationContextProvider) object;
                    provider.getContext().setProperty(Lookup.DELETED_OBJECT_PROPERTY, deletedObject);
                }
            }
        });
    }

    // логика создания объекта идентична логике сериализации :
    // 1. Создается объект с пустым конструктором
    // 2. У него вызывается setID
    // 3. Ему выставляется ApplicationContext
    // 4. У него вызывается customConstructor в котором должны создаваться все агрегированные объекты
    private Object processCreatedObject(Object object, Object parent) {
        if (object instanceof ApplicationContextHolder && parent instanceof ApplicationContextProvider) {
            ((ApplicationContextHolder) object).setContext(((ApplicationContextProvider) parent).getContext());
        }
        if (object instanceof IdentityInterface) {
            assert object instanceof ApplicationContextHolder; // пока делаем так - значит объект с Identity, но без контекста
            ((IdentityInterface) object).setID(((ApplicationContextHolder) object).getContext().idShift());
        }
        if (object instanceof CustomConstructible) {
            ((CustomConstructible) object).customConstructor();
        }
        if (object instanceof ApplicationContextProvider) {
            ApplicationContextProvider provider = (ApplicationContextProvider) object;
            provider.getContext().setProperty(Lookup.NEW_EDITABLE_OBJECT_PROPERTY, object);
        }
        return object;
    }

    public boolean canImport(TransferHandler.TransferSupport info) {
        return false;
    }

    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return false;
    }

    public void exportDone(ClientTree tree, JComponent component, Transferable trans, int action) {
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

        return thisObj != null && otherObj != null && thisObj.equals(otherObj);
    }
}
