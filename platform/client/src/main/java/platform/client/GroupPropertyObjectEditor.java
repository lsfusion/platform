package platform.client;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.PropertyDrawDescriptor;
import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.editor.base.Tristate;
import platform.client.descriptor.editor.base.TristateCheckBox;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class GroupPropertyObjectEditor extends SimplePropertyFilter implements IncrementView {

    public GroupObjectDescriptor groupObject;

    public void update(Object updateObject, String updateField) {
        updateSelection();
    }

    @Override
    public void updateTree() {
        super.updateTree();

        updateSelection();
    }

    public GroupPropertyObjectEditor(FormDescriptor form, GroupObjectDescriptor descriptor) {
        super(form, descriptor);
        groupObject = descriptor;
        initTree();
    }

    public GroupPropertyObjectEditor(List<GroupObjectDescriptor> groupObjects) {
        super(groupObjects);
        initTree();
    }

    void initTree(){
                tree.setCellRenderer(new CheckRenderer());
        tree.putClientProperty("JTree.lineStyle", "Angled");
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int row = tree.getRowForLocation(x, y);
                TreePath path = tree.getPathForRow(row);
                if (path != null) {
                    CheckNode node = (CheckNode) path.getLastPathComponent();
                    node.setSelection(node.selectedState!=Tristate.SELECTED);
                    ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
                    if (row == 0) {
                        tree.revalidate();
                        tree.repaint();
                    }
                }
            }
        });
        list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        treeModel.reload();

        IncrementDependency.add("propertyDraws", this);
        IncrementDependency.add("toDraw", this);
        IncrementDependency.add("propertyObject", this);
    }

    @Override
    DefaultMutableTreeNode getNode(Object info) {
        return new CheckNode(info);
    }

    private boolean isSelectedPropertyObject(PropertyObjectDescriptor propertyObject) {
        for(PropertyDrawDescriptor propertyDraw : form.getGroupPropertyDraws(groupObject))
            if(propertyObject.equals(propertyDraw.getPropertyObject()))
                return true;
        return false;
    }

    void updateSelection() {
        ((CheckNode)treeModel.getRoot()).updateSelection();

        tree.revalidate();
        tree.repaint();
    }

    public class CheckNode extends DefaultMutableTreeNode {

        protected Tristate selectedState = Tristate.NOT_SELECTED;

        public CheckNode(Object userObject) {
            super(userObject, true);
        }

        void setSelection(boolean select) {
            if ((children != null)) {
                // assert что группа
                Enumeration e = children.elements();
                while (e.hasMoreElements())
                    ((CheckNode) e.nextElement()).setSelection(select);
            } else
                if(userObject instanceof PropertyObjectDescriptor) {
                    if(select) { // добавляем propertyDraw
                        if(!isSelectedPropertyObject((PropertyObjectDescriptor) userObject)) {
                            PropertyDrawDescriptor propertyDraw = new PropertyDrawDescriptor((PropertyObjectDescriptor) userObject);
                            form.addToPropertyDraws(propertyDraw, groupObject);
                        }
                    } else // удаляем все с таким propertyObject
                        for(PropertyDrawDescriptor propertyDraw : form.getGroupPropertyDraws(groupObject))
                            if(propertyDraw.getPropertyObject().equals(userObject))
                                form.removeFromPropertyDraws(propertyDraw);
                }
        }

        public Tristate updateSelection() {
            selectedState = null;
            if ((children != null)) {
                // assert что группа
                Enumeration e = children.elements();
                while (e.hasMoreElements()) {
                    Tristate childState = ((CheckNode) e.nextElement()).updateSelection();
                    selectedState = selectedState==null?childState:selectedState.and(childState);
                }
            } else
                if(userObject instanceof PropertyObjectDescriptor)
                    selectedState = isSelectedPropertyObject((PropertyObjectDescriptor)userObject)?Tristate.SELECTED:Tristate.NOT_SELECTED;

            if(selectedState==null)
                selectedState = Tristate.SELECTED;
            return selectedState;
        }
    }
}

class CheckRenderer extends JPanel implements TreeCellRenderer {
    protected TristateCheckBox check;

    protected TreeLabel label;

    public CheckRenderer() {
        setLayout(null);
        add(check = new TristateCheckBox() {
            protected void onChange() {
                // ничего не делаем
            }
        });
        add(label = new TreeLabel());
        check.setBackground(UIManager.getColor("Tree.textBackground"));
        label.setForeground(UIManager.getColor("Tree.textForeground"));
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean isSelected, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, isSelected,
                expanded, leaf, row, hasFocus);
        setEnabled(tree.isEnabled());
        check.setState(((GroupPropertyObjectEditor.CheckNode) value).selectedState);
        label.setFont(tree.getFont());
        label.setText(stringValue);
        label.isSelected = isSelected;
        label.hasFocus = hasFocus;
        if (leaf) {
            label.setIcon(UIManager.getIcon("Tree.leafIcon"));
        } else if (expanded) {
            label.setIcon(UIManager.getIcon("Tree.openIcon"));
        } else {
            label.setIcon(UIManager.getIcon("Tree.closedIcon"));
        }
        return this;
    }

    public Dimension getPreferredSize() {
        Dimension d_check = check.getPreferredSize();
        Dimension d_label = label.getPreferredSize();
        return new Dimension(d_check.width + d_label.width,
                (d_check.height < d_label.height ? d_label.height
                        : d_check.height));
    }

    public void doLayout() {
        Dimension d_check = check.getPreferredSize();
        Dimension d_label = label.getPreferredSize();
        int y_check = 0;
        int y_label = 0;
        if (d_check.height < d_label.height) {
            y_check = (d_label.height - d_check.height) / 2;
        } else {
            y_label = (d_check.height - d_label.height) / 2;
        }
        // почему-то надо -2 иначе налазит на рамки
        check.setBounds(0, y_check, d_check.width, d_check.height-2);
        label.setBounds(d_check.width, y_label, d_label.width, d_label.height-2);
    }

    public void setBackground(Color color) {
        if (color instanceof ColorUIResource)
            color = null;
        super.setBackground(color);
    }

    public class TreeLabel extends JLabel {
        boolean isSelected;

        boolean hasFocus;

        public TreeLabel() {
        }

        public void setBackground(Color color) {
            if (color instanceof ColorUIResource)
                color = null;
            super.setBackground(color);
        }

        public void paint(Graphics g) {
            String str;
            if ((str = getText()) != null) {
                if (0 < str.length()) {
                    if (isSelected) {
                        g.setColor(UIManager.getColor("Tree.selectionBackground"));
                    } else {
                        g.setColor(UIManager.getColor("Tree.textBackground"));
                    }
                    Dimension d = getPreferredSize();
                    int imageOffset = 0;
                    Icon currentI = getIcon();
                    if (currentI != null) {
                        imageOffset = currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
                    }
                    g.fillRect(imageOffset, 0, d.width - 1 - imageOffset, d.height);
                    if (hasFocus) {
                        g.setColor(UIManager.getColor("Tree.selectionBorderColor"));
                        //g.drawRect(imageOffset, 0, d.width - 1 - imageOffset, d.height - 1);
                    }
                }
            }
            super.paint(g);
        }

        public Dimension getPreferredSize() {
            Dimension retDimension = super.getPreferredSize();
            if (retDimension != null) {
                retDimension = new Dimension(retDimension.width + 3,
                        retDimension.height);
            }
            return retDimension;
        }
    }
}

