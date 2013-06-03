package lsfusion.client.descriptor.editor;

import lsfusion.base.context.IncrementView;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.GroupObjectDescriptor;
import lsfusion.client.descriptor.PropertyDrawDescriptor;
import lsfusion.client.descriptor.PropertyObjectDescriptor;
import lsfusion.client.descriptor.editor.base.Tristate;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

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
                    GroupPropertyObjectEditor.CheckNode node = (GroupPropertyObjectEditor.CheckNode) path.getLastPathComponent();
                    node.setSelection(node.selectedState== Tristate.NOT_SELECTED);
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

        form.addDependency("propertyDraws", this);
        form.addDependency("toDraw", this);
        form.addDependency("propertyObject", this);
    }

    @Override
    DefaultMutableTreeNode getNode(Object info) {
        return new GroupPropertyObjectEditor.CheckNode(info);
    }

    private boolean isSelectedPropertyObject(PropertyObjectDescriptor propertyObject) {
        for(PropertyDrawDescriptor propertyDraw : form.getGroupPropertyDraws(groupObject))
            if(propertyObject.equals(propertyDraw.getPropertyObject()))
                return true;
        return false;
    }

    void updateSelection() {
        ((GroupPropertyObjectEditor.CheckNode)treeModel.getRoot()).updateSelection();

        tree.revalidate();
        tree.repaint();
        expandTree(tree, (DefaultMutableTreeNode)treeModel.getRoot());
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
                    ((GroupPropertyObjectEditor.CheckNode) e.nextElement()).setSelection(select);
            } else
                if(userObject instanceof PropertyObjectDescriptor) {
                    if(select) { // добавляем propertyDraw
                        if(!isSelectedPropertyObject((PropertyObjectDescriptor) userObject)) {
                            PropertyDrawDescriptor propertyDraw = new PropertyDrawDescriptor(form.getContext(), (PropertyObjectDescriptor) userObject);
                            if(groupObject != null && !groupObject.equals(propertyDraw.getGroupObject(form.groupObjects)))
                                propertyDraw.setToDraw(groupObject);
                            form.addToPropertyDraws(propertyDraw);
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
                    Tristate childState = ((GroupPropertyObjectEditor.CheckNode) e.nextElement()).updateSelection();
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

