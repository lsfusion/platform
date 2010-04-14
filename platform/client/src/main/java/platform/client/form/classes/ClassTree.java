package platform.client.form.classes;

import platform.client.ExpandingTreeNode;
import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientObjectClass;

import javax.swing.*;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.io.IOException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.*;

abstract class ClassTree extends JTree {

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

    // ID нужен для того, чтобы правильно работал equals
    private final int ID;

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ClassTree && ((ClassTree) o).ID == this.ID;
    }

    ClassTree(int treeID, ClientClass rootClass) {
    
        ID = treeID;

        currentClass = rootClass;
        currentNode = new DefaultMutableTreeNode(rootClass);

        setToggleClickCount(-1);
        setBorder(new EtchedBorder(EtchedBorder.LOWERED));

        model = new DefaultTreeModel(currentNode);
        setModel(model);

        addTreeExpansionListener(new TreeExpansionListener() {

            public void treeExpanded(TreeExpansionEvent event) {
                try {
                    addNodeElements((DefaultMutableTreeNode)event.getPath().getLastPathComponent());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при получении потомков класса", e);
                }
            }

            public void treeCollapsed(TreeExpansionEvent event) {}

        });

        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    try {
                        changeCurrentClass(getSelectedClass(), getSelectedNode());
                        currentClassChanged();
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении текущего класса", e);
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    try {
                        currentClassChanged();
                    } catch (IOException e) {
                        throw new RuntimeException("Ошибка при изменении текущего класса", e);
                    }
                }
            }
        });

        if (rootClass.hasChilds()) {
            currentNode.add(new ExpandingTreeNode());
            expandPath(new TreePath(currentNode));
        }

        this.setSelectionRow(0);

    }

    private void changeCurrentClass(ClientObjectClass cls, DefaultMutableTreeNode clsNode) throws IOException {

        if (cls != null) {

            currentClass = cls;
            currentNode = clsNode;

            currentClassChanged();

            //запускаем событие изменение фонта, чтобы сбросить кэш в дереве, который расчитывает ширину поля
            //собственно оно само вызывает перерисовку
            firePropertyChange("font", false, true);
        }
    }

    protected abstract void currentClassChanged() throws IOException;

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

        java.util.List<ClientClass> classes = getChildClasses(parentClass);

        for (ClientClass cls : classes) {

            DefaultMutableTreeNode node;
            node = new DefaultMutableTreeNode(cls, cls.hasChilds());
            parent.add(node);

            if (cls.hasChilds())
                node.add(new ExpandingTreeNode());

        }

        model.reload(parent);
    }

    protected abstract java.util.List<ClientClass> getChildClasses(ClientObjectClass parentClass) throws IOException;

    DefaultMutableTreeNode getSelectedNode() {

        TreePath path = getSelectionPath();
        if (path == null) return null;

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    ClientObjectClass getSelectedClass() {

        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return null;

        Object nodeObject = node.getUserObject();
        if (! (nodeObject instanceof ClientClass)) return null;
        return (ClientObjectClass) nodeObject;
    }

}