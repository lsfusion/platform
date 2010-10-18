package platform.client.descriptor.view;

import platform.client.ClassFilteredAction;
import platform.client.ClientTree;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.nodes.FormNode;
import platform.client.descriptor.nodes.PlainTextNode;
import platform.client.descriptor.nodes.actions.AddableTreeNode;
import platform.client.descriptor.nodes.actions.DeletableTreeNode;
import platform.client.descriptor.nodes.actions.EditableTreeNode;
import platform.client.navigator.ClientNavigator;
import platform.interop.serialization.RemoteDescriptorInterface;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class FormDescriptorView extends JPanel implements IncrementView {

    private FormDescriptor form;

    private ClientTree tree;
    private DefaultTreeModel model;

    private EditorView view;

    private RemoteDescriptorInterface remote;
    private JButton previewBtn;
    private JButton saveBtn;
    private ClientNavigator navigator;
    private TreePath editPath;

    public FormDescriptorView(ClientNavigator iNavigator, RemoteDescriptorInterface remote) {
        this.navigator = iNavigator;

        this.remote = remote;

        setLayout(new BorderLayout());

        view = new EditorView();

        tree = new ClientTree();
        tree.setCellRenderer(new MyTreeCellRenderer());

        tree.setDropMode(DropMode.ON);
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
        IncrementDependency.add("caption", this);
        IncrementDependency.add("propertyObject", this);
        IncrementDependency.add("toDraw", this);
        IncrementDependency.add(this, "form", this);
    }

    public void setForm(FormDescriptor form) {
        this.form = form;

        previewBtn.setEnabled(form != null);
        saveBtn.setEnabled(form != null);

        IncrementDependency.update(this, "form");
    }

    public void update(Object updateObject, String updateField) {

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
    }

    private void addActions(FormNode formNode) {
        formNode.addSubTreeAction(
                new ClassFilteredAction("Редактировать", EditableTreeNode.class) {
                    public void actionPerformed(ActionEvent e) {
                        editPath(tree.getSelectionPath());
                    }
                });

        formNode.addSubTreeAction(
                new ClassFilteredAction("Добавить", AddableTreeNode.class) {
                    public void actionPerformed(ActionEvent e) {
                        if (view.validateEditor()) {
                            DefaultMutableTreeNode node = tree.getSelectionNode();
                            if (node instanceof AddableTreeNode) {
                                Object[] addedObjectPath = ((AddableTreeNode) node).addNewElement(tree.getSelectionPath());

                                editPath(tree.findPathByUserObjects(addedObjectPath));
                            }
                        }
                    }
                });

        formNode.addSubTreeAction(
                new ClassFilteredAction("Удалить", DeletableTreeNode.class) {
                    public void actionPerformed(ActionEvent e) {
                        DefaultMutableTreeNode node = tree.getSelectionNode();
                        if (node instanceof DeletableTreeNode) {
                            if (editPath != null && editPath.equals(tree.getSelectionPath())) {
                                view.removeEditor(); 
                            }
                            ((DeletableTreeNode) node).deleteNode(tree.getSelectionPath());
                        }
                    }
                });
    }

    public void editPath(TreePath path) {
        if (path == null) {
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node instanceof EditableTreeNode) {
            EditableTreeNode editingNode = (EditableTreeNode) node;
            if (view.setEditor(editingNode.createEditor(form, remote))) {
                editPath = path;

                update(null, null);
            }
        }
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

            if (ClientTree.comparePathsByUserObjects(editPath, tree.getPathToRoot((TreeNode) value))) {
                setBackgroundSelectionColor(Color.YELLOW.darker().darker());
                setBackgroundNonSelectionColor(Color.YELLOW);
            } else {
                setBackgroundSelectionColor(backgroundSelectionColor);
                setBackgroundNonSelectionColor(backgroundNonSelectionColor);
            }

            return renderer;
        }
    }
}
