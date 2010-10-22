package platform.client.descriptor.view;

import platform.client.ClassFilteredAction;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.nodes.FormNode;
import platform.client.descriptor.nodes.PlainTextNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.lookup.Lookup;
import platform.client.navigator.ClientNavigator;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ClientTreeNode;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Enumeration;

public class FormDescriptorView extends JPanel implements IncrementView, Lookup.LookupResultChangeListener {

    private FormDescriptor form;

    private ClientTree tree;
    private DefaultTreeModel model;

    private EditorView view;

    private RemoteDescriptorInterface remote;
    private JButton previewBtn;
    private JButton saveBtn;
    private ClientNavigator navigator;

    private Object objectToEdit;
    private Object editingObject;
    private Lookup lookup = Lookup.getDefault();

    public FormDescriptorView(ClientNavigator iNavigator, RemoteDescriptorInterface remote) {
        globalInstance = this;

        this.navigator = iNavigator;

        this.remote = remote;

        setLayout(new BorderLayout());

        view = new EditorView();

        tree = new ClientTree();
        tree.setCellRenderer(new MyTreeCellRenderer());

        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setDragEnabled(true);

        previewBtn = new JButton("Предпросмотр формы");
        previewBtn.setEnabled(false);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreviewDialog dlg = new PreviewDialog(navigator, form);
                dlg.setBounds(SwingUtilities.windowForComponent(FormDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        saveBtn = new JButton("Сохранить форму");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    navigator.remoteNavigator.saveForm(form.getID(), FormDescriptor.serialize(form));
                } catch (IOException ioe) {
                    throw new RuntimeException("Не могу сохранить форму.", ioe);
                }
            }
        });

        JPanel commandPanel = new JPanel();
        commandPanel.add(previewBtn);
        commandPanel.add(saveBtn);

        JPanel formTreePanel = new JPanel();
        formTreePanel.setLayout(new BorderLayout());

        formTreePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        formTreePanel.add(commandPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formTreePanel, view);
        splitPane.setResizeWeight(0.3);

        add(splitPane, BorderLayout.CENTER);

        IncrementDependency.add("groupObjects", this);
        IncrementDependency.add("propertyDraws", this);
        IncrementDependency.add("children", this);
        IncrementDependency.add("objects", this);
        IncrementDependency.add("caption", this);
        IncrementDependency.add("title", this);
        IncrementDependency.add("description", this);
        IncrementDependency.add("overridenCaption", this);
        IncrementDependency.add("propertyObject", this);
        IncrementDependency.add("toDraw", this);
        IncrementDependency.add("fixedFilters", this);
        IncrementDependency.add("regularFilterGroups", this);
        IncrementDependency.add("filters", this);
        IncrementDependency.add("filter", this);
        IncrementDependency.add("op1", this);
        IncrementDependency.add("op2", this);
        IncrementDependency.add("property", this);
        IncrementDependency.add(this, "form", this);

        lookup.addLookupResultChangeListener(Lookup.NEW_EDITABLE_OBJECT_PROPERTY, this);
        lookup.addLookupResultChangeListener(Lookup.DELETED_OBJECT_PROPERTY, this);
    }

    public void resultChanged(String name, Object oldValue, Object newValue) {
        if (name.equals(Lookup.NEW_EDITABLE_OBJECT_PROPERTY)) {
            if (newValue != null && newValue != editingObject && view.validateEditor()) {
                objectToEdit = newValue;
                removeEditor();
            }
        } else if (name.equals(Lookup.DELETED_OBJECT_PROPERTY)) {
            if (newValue != null && newValue == editingObject) {
                removeEditor();
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

    public void setForm(FormDescriptor form) {
        if (this.form != form) {
            this.form = form;
            view.removeEditor();
        }

        previewBtn.setEnabled(form != null);
        saveBtn.setEnabled(form != null);

        IncrementDependency.update(this, "form");
    }

    public void update(Object updateObject, String updateField) {
        SwingUtilities.invokeLater(new OnUpdate());
    }

    private void addActions(FormNode formNode) {
        formNode.addSubTreeAction(
                new ClassFilteredAction("Редактировать", EditableTreeNode.class) {
                    public void actionPerformed(ClientTreeActionEvent e) {
                        Object editObject = ((ClientTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject();
                        lookup.setProperty(Lookup.NEW_EDITABLE_OBJECT_PROPERTY, editObject);
                    }
                });
    }

    class MyTreeCellRenderer extends DefaultTreeCellRenderer {
        private Color backgroundSelectionColor;
        private Color backgroundNonSelectionColor;

        public MyTreeCellRenderer() {
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

    public static FormDescriptorView globalInstance;

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
                    EditableTreeNode editableNode = (EditableTreeNode)node;
                    view.setEditor(editableNode.createEditor(form, remote));
                    editingObject = objectToEdit;
                    objectToEdit = null;
                    updateNow();
                    break;
                }
            }
        }
    }
}

