package platform.client.descriptor.view;

import platform.client.tree.ClassFilteredAction;
import platform.client.descriptor.FormDescriptor;
import platform.base.context.IncrementView;
import platform.client.descriptor.nodes.FormNode;
import platform.client.descriptor.nodes.PlainTextNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.base.context.Lookup;
import platform.client.navigator.ClientNavigator;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ClientTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Enumeration;

// предполагается, что будет ровно один FormDescriptorView, которому будут говорить сверху, чтобы он отображал разные FormDescriptor'ы
public class FormDescriptorView extends JPanel implements IncrementView, Lookup.LookupResultChangeListener {

    private FormDescriptor form;

    private ClientTree tree;
    private DefaultTreeModel model;

    private EditorView view;

    private JButton previewBtn;
    private JButton saveBtn;
    private JButton cancelBtn;
    private ClientNavigator clientNavigator;
    private final NavigatorDescriptorView parent;

    private Object objectToEdit;
    private Object editingObject;

    public FormDescriptorView(ClientNavigator iClientNavigator, NavigatorDescriptorView iParent) {
        clientNavigator = iClientNavigator;
        parent = iParent;

        setLayout(new BorderLayout());

        view = new EditorView();

        tree = new ClientTree();
        tree.setCellRenderer(new VisualSetupTreeCellRenderer());

        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setDragEnabled(true);

        previewBtn = new JButton("Предпросмотр формы");
        previewBtn.setEnabled(false);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreviewDialog dlg = new PreviewDialog(clientNavigator, form);
                dlg.setBounds(SwingUtilities.windowForComponent(FormDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        saveBtn = new JButton("Сохранить форму");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    clientNavigator.remoteNavigator.saveForm(form.getID(), FormDescriptor.serialize(form));
                } catch (IOException ioe) {
                    throw new RuntimeException("Не могу сохранить форму.", ioe);
                }
            }
        });

        cancelBtn = new JButton("Отменить изменения");
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    parent.reopenForm(form.getID());
                } catch (IOException ioe) {
                    throw new RuntimeException("Не могу открыть форму.", ioe);
                }
            }
        });

        JPanel commandPanel = new JPanel();
        commandPanel.add(previewBtn);
        commandPanel.add(saveBtn);
        commandPanel.add(Box.createRigidArea(new Dimension(20, 5)));
        commandPanel.add(cancelBtn);

        JPanel formTreePanel = new JPanel();
        formTreePanel.setLayout(new BorderLayout());

        formTreePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        formTreePanel.add(commandPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formTreePanel, view);
        splitPane.setResizeWeight(0.01);

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

    public void setForm(FormDescriptor iform) {
        if (form != iform) {
            if (form != null) {
                removeDependencies(this.form);
            }
            this.form = iform;
            view.removeEditor();
            objectToEdit = form;
        }

        previewBtn.setEnabled(form != null);
        saveBtn.setEnabled(form != null);
        cancelBtn.setEnabled(form != null);

        if (form != null) {
            addDependencies(form);
            form.updateDependency(this, "form");
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

    public void update(Object updateObject, String updateField) {
        SwingUtilities.invokeLater(new OnUpdate());
    }

    private void addActions(FormNode formNode) {
        formNode.addSubTreeAction(
                new ClassFilteredAction("Редактировать", EditableTreeNode.class) {
                    public void actionPerformed(ClientTreeActionEvent e) {
                        Object editObject = ((ClientTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject();
                        form.getContext().setProperty(Lookup.NEW_EDITABLE_OBJECT_PROPERTY, editObject);
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

