package platform.client.descriptor.view;

import platform.base.context.IncrementView;
import platform.base.context.Lookup;
import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.navigator.*;
import platform.client.tree.ClientTree;
import platform.client.tree.ClientTreeAction;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ClientTreeNode;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NavigatorDescriptorView extends JPanel {
    private FormDescriptorView formView;
    private VisualSetupNavigator visualNavigator;

    //todo: в будущем формы надо создавать на сервере и это убрать...
    private Map<Integer, FormDescriptor> forms = new HashMap<Integer, FormDescriptor>();

    private JButton previewBtn;
    private JButton saveBtn;
    private JButton cancelBtn;

    public NavigatorDescriptorView(final ClientNavigator clientNavigator) {

        setLayout(new BorderLayout());

        visualNavigator = new VisualSetupNavigator(clientNavigator.remoteNavigator);

        formView = new FormDescriptorView();

        previewBtn = new JButton("Предпросмотр формы");
        previewBtn.setEnabled(false);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreviewDialog dlg = new PreviewDialog(clientNavigator, formView.getForm());
                dlg.setBounds(SwingUtilities.windowForComponent(NavigatorDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        saveBtn = new JButton("Сохранить изменения");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    DataOutputStream dataStream = new DataOutputStream(outStream);

                    dataStream.writeInt(forms.size());
                    for (FormDescriptor form : forms.values()) {
                        int oldSize = outStream.size();
                        form.serialize(dataStream);
                        int formSize = outStream.size() - oldSize;
                        dataStream.writeInt(formSize);
                    }

                    visualNavigator.remoteNavigator.saveForms(outStream.toByteArray());

                    forms.clear();
                    reopenForm(formView.getForm().ID);
                } catch (IOException ioe) {
                    throw new RuntimeException("Не могу сохранить форму.", ioe);
                }
            }
        });

        cancelBtn = new JButton("Отменить все изменения");
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    forms.clear();
                    openForm(formView.getForm().getID());
                } catch (IOException ioe) {
                    throw new RuntimeException("Не могу открыть форму.", ioe);
                }
            }
        });

        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.X_AXIS));
        commandPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        commandPanel.add(previewBtn);
        commandPanel.add(saveBtn);
        commandPanel.add(Box.createRigidArea(new Dimension(20, 5)));
        commandPanel.add(cancelBtn);
        commandPanel.add(Box.createHorizontalGlue());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(visualNavigator), formView);
        splitPane.setResizeWeight(0.1);

        add(splitPane, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.SOUTH);
    }

    public void reopenForm(int ID) throws IOException {
        forms.remove(ID);
        openForm(ID);
    }

    public void openForm(int ID) throws IOException {
        if (!forms.containsKey(ID)) {
            forms.put(ID, FormDescriptor.deserialize(visualNavigator.remoteNavigator.getRichDesignByteArray(ID),
                                                     visualNavigator.remoteNavigator.getFormEntityByteArray(ID)));
        }
        formView.setForm(forms.get(ID));
        updateUI();

        setupActionButtons(true);
    }

    private void removeForm(FormDescriptor form) {
        form.getContext().setProperty(Lookup.DELETED_OBJECT_PROPERTY, form);
        forms.remove(form.getID());

        setupActionButtons(false);
    }

    private void setupActionButtons(boolean enabled) {
        previewBtn.setEnabled(enabled);
        saveBtn.setEnabled(enabled);
        cancelBtn.setEnabled(enabled);
    }

    private boolean isFormChanged(int formID) {
        return forms.containsKey(formID);
    }

    private class VisualSetupNavigator extends AbstractNavigator {

        public VisualSetupNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);

            tree.setDropMode(DropMode.ON_OR_INSERT);
            tree.setDragEnabled(true);
            tree.setCellRenderer(new VisualSetupNavigatorRenderer());

            tree.rootNode.addSubTreeAction(
                    new ClientTreeAction("Создать новую форму") {
                        @Override
                        public void actionPerformed(ClientTreeActionEvent e) {
                            ClientTreeNode node = e.getNode();

                            FormDescriptor newForm = new FormDescriptor(Main.generateNewID());
                            forms.put(newForm.getID(), newForm);

                            //раскрываем, чтобы загрузить узлы с сервера...
                            tree.expandPath(tree.getPathToRoot(node));

                            node.add(new NavigatorTreeNode(new NewNavigatorForm(newForm), true));

                            tree.getModel().reload(node);

                            formView.setForm(newForm);
                        }

                        @Override
                        public boolean isApplicable(TreePath path) {
                            ClientTreeNode node = ClientTree.getNode(path);
                            if (node != null) {
                                Object nodeObject = node.getUserObject();
                                return nodeObject instanceof ClientNavigatorElement;
                            }
                            return false;
                        }
                    });

            tree.rootNode.addSubTreeAction(
                    new ClientTreeAction("Отменить изменения") {
                        @Override
                        public void actionPerformed(ClientTreeActionEvent e) {
                            ClientTreeNode node = e.getNode();
                            Object userObject = node.getUserObject();
                            if (userObject instanceof ClientNavigatorForm) {
                                ClientNavigatorForm navigatorForm = (ClientNavigatorForm) userObject;
                                try {
                                    FormDescriptor form = forms.get(navigatorForm.ID);
                                    if (form == formView.getForm()) {
                                        reopenForm(form.ID);
                                    } else {
                                        forms.remove(form.ID);
                                    }
                                } catch (IOException ioe) {
                                    throw new RuntimeException("Ошибка при отмене формы.");
                                }
                            }
                        }

                        @Override
                        public boolean isApplicable(TreePath path) {
                            ClientTreeNode node = ClientTree.getNode(path);
                            if (node != null) {
                                Object nodeObject = node.getUserObject();

                                if (nodeObject instanceof ClientNavigatorForm) {
                                    ClientNavigatorForm navigatorForm = (ClientNavigatorForm) nodeObject;
                                    return forms.containsKey(navigatorForm.ID);
                                }
                            }
                            return false;
                        }
                    });

            tree.rootNode.addSubTreeAction(
                    new ClientTreeAction("Удалить") {
                        @Override
                        public void actionPerformed(ClientTreeActionEvent e) {
                            ClientTreeNode child = e.getNode();
                            ClientTreeNode parent = (ClientTreeNode) e.getNode().getParent();

                            parent.remove(child);
                            tree.getModel().reload(parent);

                            Object userObject = child.getUserObject();
                            if (userObject instanceof NewNavigatorForm) {
                                NewNavigatorForm formElement = (NewNavigatorForm) userObject;
                                removeForm(formElement.form);
                            }
                        }
                    });
        }


        @Override
        public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
            NavigatorDescriptorView.this.openForm(element.ID);
        }

        class VisualSetupNavigatorRenderer extends ClientTree.ClientTreeCellRenderer {

            private Color textNonSelectionColor;

            public VisualSetupNavigatorRenderer() {
                super();

                textNonSelectionColor = getTextNonSelectionColor();
            }

            @Override
            public Component getTreeCellRendererComponent(JTree iTree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                setTextNonSelectionColor(textNonSelectionColor);

                if (value instanceof NavigatorTreeNode) {
                    NavigatorTreeNode node = (NavigatorTreeNode) value;
                    Object userObject = node.getUserObject();
                    if (userObject instanceof ClientNavigatorForm) {
                        ClientNavigatorForm form = (ClientNavigatorForm) userObject;
                        if (isFormChanged(form.ID)) {
                            setTextNonSelectionColor(Color.blue);
                        }
                    }
                }

                return super.getTreeCellRendererComponent(iTree, value, sel, expanded, leaf, row, hasFocus);
            }
        }

        class NewNavigatorForm extends ClientNavigatorForm implements IncrementView {

            private final FormDescriptor form;

            public NewNavigatorForm(FormDescriptor form) {
                super();
                this.form = form;
                ID = form.getID();

                form.addDependency(form, "caption", this);
            }

            public void update(Object updateObject, String updateField) {
                tree.updateUI();
            }

            @Override
            public String toString() {
                return form.getCaption();
            }
        }
    }
}
