package lsfusion.client.form.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.RmiQueue;
import lsfusion.client.logics.classes.ClientClass;
import lsfusion.client.logics.classes.ClientObjectClass;
import lsfusion.client.tree.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public abstract class ClassTree extends ClientTree {

    // текущие выбранные узлы в дереве
    private DefaultMutableTreeNode currentNode;
    public DefaultMutableTreeNode getCurrentNode() {
        return currentNode;
    }

    private ClientClass currentClass;
    public Object getCurrentClass() {
        return currentClass;
    }

    private final DefaultTreeModel model;

    // compID нужен для того, чтобы правильно работал equals
    private final int ID;

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClassTree && ((ClassTree) o).ID == this.ID;
    }

    public ClassTree(int treeID, ClientClass rootClass) {
        super();
    
        ID = treeID;

        currentClass = rootClass;
        currentNode = new ClientTreeNode(rootClass, true, ClientTreeNode.SUB_TREE,
                new ClientTreeAction(ClientResourceBundle.getString("form.classes.open"), true){
                    public void actionPerformed(ClientTreeActionEvent e){
                        RmiQueue.runAction(new Runnable() {
                            @Override
                            public void run() {
                                changeCurrentClass(getSelectionClass(), getSelectionNode());
                                currentClassChanged();
                            }
                        });
                    }
                });

        
        setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        model = new DefaultTreeModel(currentNode);
        setModel(model);

        addTreeExpansionListener(new TreeExpansionListener() {

            public void treeExpanded(TreeExpansionEvent event) {
                try {
                    addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                } catch (IOException e) {
                    throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.descendants.of.the.class"), e);
                }
            }

            public void treeCollapsed(TreeExpansionEvent event) {}

        });

        if (rootClass.hasChildren()) {
            currentNode.add(new ExpandingTreeNode());
            expandPath(new TreePath(currentNode));
        }

        // раскрываем все дерево
        for (int i = 0; i < getRowCount(); i++)
            expandRow(i);

        this.setSelectionRow(0);

    }

    private void changeCurrentClass(ClientObjectClass cls, DefaultMutableTreeNode clsNode) {

        if (cls != null) {

            currentClass = cls;
            currentNode = clsNode;

            currentClassChanged();

            //запускаем событие изменение фонта, чтобы сбросить кэш в дереве, который расчитывает ширину поля
            //собственно оно само вызывает перерисовку
            firePropertyChange("font", false, true);
        }
    }

    protected abstract void currentClassChanged();

    @Override
    public void updateUI() {
        super.updateUI();

        //приходится в updateUI это засовывать, иначе при изменении UI - нифига не перерисовывает
        setCellRenderer(new DefaultTreeCellRenderer() {

            Font defaultFont;

            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row,
                                                          boolean hasFocus) {
                if (defaultFont == null) {
                    Component comp = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                    defaultFont = comp.getFont();
                }

                setFont(defaultFont);

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node != null) {

                    if (node == currentNode)
                        setFont(getFont().deriveFont(Font.BOLD));

                }

                return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        });
    }

    private void addNodeElements(DefaultMutableTreeNode parent) throws IOException {

        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)parent.getFirstChild();

        if (! (firstChild instanceof ExpandingTreeNode)) return;
        parent.removeAllChildren();

        Object nodeObject = parent.getUserObject();
        if (nodeObject == null || ! (nodeObject instanceof ClientClass) ) return;

        ClientObjectClass parentClass = (ClientObjectClass) nodeObject;

        List<ClientObjectClass> classes = parentClass.getChildren();

        for (ClientObjectClass objectClass : classes) {

            DefaultMutableTreeNode node;
            node = new ClientTreeNode(objectClass, objectClass.hasChildren());
            parent.add(node);

            if (objectClass.hasChildren())
                node.add(new ExpandingTreeNode());

        }

        model.reload(parent);
    }

    public ClientObjectClass getSelectionClass() {

        DefaultMutableTreeNode node = getSelectionNode();
        if (node == null) return null;

        Object nodeObject = node.getUserObject();
        if (! (nodeObject instanceof ClientClass)) return null;
        return (ClientObjectClass) nodeObject;
    }

    public boolean setSelectedClass(ClientObjectClass cls) {
        return setNodeSelectedClass((DefaultMutableTreeNode) getModel().getRoot(), cls);
    }

    private boolean setNodeSelectedClass(DefaultMutableTreeNode node, ClientObjectClass cls) {

        if (cls.equals(node.getUserObject())) {

            TreePath selectedPath = new TreePath(node.getPath());
            setSelectionPath(selectedPath);
            scrollPathToVisible(selectedPath);
            changeCurrentClass(cls, node);
            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (setNodeSelectedClass((DefaultMutableTreeNode)node.getChildAt(i), cls))
                return true;
        }

        return false;
    }

}