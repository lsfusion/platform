package platform.client.descriptor.view;

import platform.base.context.IncrementView;
import platform.base.context.Lookup;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.nodes.FormNode;
import platform.client.descriptor.nodes.PlainTextNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.tree.ClassFilteredAction;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ClientTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Enumeration;

// предполагается, что будет ровно один FormDescriptorView, которому будут говорить сверху, чтобы он отображал разные FormDescriptor'ы
public class FormDescriptorView extends JPanel implements IncrementView, Lookup.LookupResultChangeListener {

    private FormDescriptor form;

    private ClientTree tree;
    private DefaultTreeModel model;

    private EditorView view;

    private Object objectToEdit;
    private Object editingObject;

    public FormDescriptorView() {
        setLayout(new BorderLayout());

        view = new EditorView();

        tree = new ClientTree();
        tree.setCellRenderer(new VisualSetupTreeCellRenderer());

        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setDragEnabled(true);

        JPanel formTreePanel = new JPanel();
        formTreePanel.setLayout(new BorderLayout());

        formTreePanel.add(new JScrollPane(tree), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formTreePanel, view);
        splitPane.setResizeWeight(0.3);

        add(splitPane, BorderLayout.CENTER);
    }

    public void resultChanged(String name, Object oldValue, Object value) {
        if (name.equals(Lookup.NEW_EDITABLE_OBJECT_PROPERTY)) {
            if (value != null && value != editingObject && view.validateEditor()) {
                objectToEdit = value;
                removeEditor();
            }
        } else if (name.equals(Lookup.DELETED_OBJECT_PROPERTY)) {
            if (value != null && value == editingObject) {
                removeEditor();
                if (value == form) {
                    setForm(null);
                }
            }
        }
    }

    private void updateNow() {
        update(null, null);
    }

    private void removeEditor() {
        editingObject = null;
        view.removeEditor();
        updateNow();
    }

    public FormDescriptor getForm() {
        return form;
    }

    public void setForm(FormDescriptor iform) {
        if (form != iform) {
            if (form != null) {
                removeDependencies(this.form);
            }
            this.form = iform;
            view.removeEditor();
            objectToEdit = form;
        }

        if (form != null) {
            trackUpdates = false;
            addDependencies(form);
            form.updateDependency(this, "form");
            trackUpdates = true;
        }
    }

    private void addDependencies(FormDescriptor form) {
        form.addDependency("groupObjects", this);
        form.addDependency("treeGroups", this);
        form.addDependency("groups", this);
        form.addDependency("propertyDraws", this);
        form.addDependency("children", this);
        form.addDependency("objects", this);
        form.addDependency("caption", this);
        form.addDependency("title", this);
        form.addDependency("description", this);
        form.addDependency("propertyObject", this);
        form.addDependency("toDraw", this);
        form.addDependency("fixedFilters", this);
        form.addDependency("regularFilterGroups", this);
        form.addDependency("filters", this);
        form.addDependency("filter", this);
        form.addDependency("op1", this);
        form.addDependency("op2", this);
        form.addDependency("value", this);
        form.addDependency("property", this);
        form.addDependency("baseClass", this);
        form.addDependency(this, "form", this);

        form.getContext().addLookupResultChangeListener(Lookup.NEW_EDITABLE_OBJECT_PROPERTY, this);
        form.getContext().addLookupResultChangeListener(Lookup.DELETED_OBJECT_PROPERTY, this);
    }

    private void removeDependencies(FormDescriptor form) {
        form.removeDependency(this);

        form.getContext().removeLookupResultChangeListener(Lookup.NEW_EDITABLE_OBJECT_PROPERTY, this);
        form.getContext().removeLookupResultChangeListener(Lookup.DELETED_OBJECT_PROPERTY, this);
    }

    private boolean trackUpdates = false;
    private boolean updated = false;
    public void update(Object updateObject, String updateField) {
        if (trackUpdates && updateObject != null && updateField != null) {
            setUpdated(true);
        }
        SwingUtilities.invokeLater(new OnUpdate());
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
        if (form != null) {
            form.updateDependency(form, "updated");
        }
    }

    public boolean getUpdated() {
        return updated;
    }

    private void addActions(FormNode formNode) {
        formNode.addSubTreeAction(
                new ClassFilteredAction("Редактировать", EditableTreeNode.class) {
                    public void actionPerformed(ClientTreeActionEvent e) {
                        Object editObject = ((ClientTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject();
                        form.getContext().setProperty(Lookup.NEW_EDITABLE_OBJECT_PROPERTY, editObject);
                    }

                    @Override
                    public boolean canBeDefault(TreePath path) {
                        return true;
                    }
                });
    }

    class VisualSetupTreeCellRenderer extends ClientTree.ClientTreeCellRenderer {
        private Color backgroundSelectionColor;
        private Color backgroundNonSelectionColor;

        public VisualSetupTreeCellRenderer() {
            super();

            backgroundSelectionColor = getBackgroundSelectionColor();
            backgroundNonSelectionColor = getBackgroundNonSelectionColor();
        }

        @Override
        public Component getTreeCellRendererComponent(JTree iTree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component renderer = super.getTreeCellRendererComponent(iTree, value, sel, expanded, leaf, row, hasFocus);

            if (editingObject != null && editingObject == ((ClientTreeNode) value).getUserObject()) {
                setBackgroundSelectionColor(Color.YELLOW.darker().darker());
                setBackgroundNonSelectionColor(Color.YELLOW);
            } else {
                setBackgroundSelectionColor(backgroundSelectionColor);
                setBackgroundNonSelectionColor(backgroundNonSelectionColor);
            }

            return renderer;
        }
    }

    private class OnUpdate implements Runnable {
        public void run() {
            TreeNode refreshNode;
            if (form != null) {
                FormNode rootNode = new FormNode(form);
                addActions(rootNode);

                refreshNode = rootNode;
            } else {
                refreshNode = new PlainTextNode("Форма не выбрана");
            }

            model = new DefaultTreeModel(refreshNode);
            tree.setModelPreservingState(model);

            if (objectToEdit != null) {
                editNewObject();
            }
        }

        private void editNewObject() {
            Enumeration<ClientTreeNode> nodes = ((ClientTreeNode) model.getRoot()).depthFirstEnumeration();
            while (nodes.hasMoreElements()) {
                ClientTreeNode node = nodes.nextElement();
                if (node.getUserObject() == objectToEdit && node instanceof EditableTreeNode) {
                    EditableTreeNode editableNode = (EditableTreeNode) node;
                    view.setEditor(editableNode.createEditor(form));
                    editingObject = objectToEdit;
                    objectToEdit = null;
                    updateNow();
                    break;
                }
            }
        }
    }
}

