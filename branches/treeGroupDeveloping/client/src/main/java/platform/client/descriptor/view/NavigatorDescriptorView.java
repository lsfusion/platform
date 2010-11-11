package platform.client.descriptor.view;

import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.increment.IncrementView;
import platform.client.lookup.Lookup;
import platform.client.navigator.*;
import platform.client.tree.ClientTreeAction;
import platform.client.tree.ClientTreeActionEvent;
import platform.client.tree.ClientTreeNode;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class NavigatorDescriptorView extends JPanel {
    private FormDescriptorView formView;
    private VisualSetupNavigator visualNavigator;

    //todo: в будущем формы надо создавать на сервере и это убрать...
    private Set<Integer> newFormsIds = new HashSet<Integer>();

    public NavigatorDescriptorView(final ClientNavigator iNavigator) {

        setLayout(new BorderLayout());

        visualNavigator = new VisualSetupNavigator(iNavigator.remoteNavigator);

        formView = new FormDescriptorView(iNavigator, this);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, visualNavigator, formView);
        splitPane.setResizeWeight(0.1);
        add(splitPane, BorderLayout.CENTER);
    }

    public void openForm(int ID) throws IOException {
        if (newFormsIds.contains(ID)) {
            formView.setForm(new FormDescriptor(ID));
        } else {
            FormDescriptor formDescriptor = FormDescriptor.deserialize(visualNavigator.remoteNavigator.getRichDesignByteArray(ID),
                                                                       visualNavigator.remoteNavigator.getFormEntityByteArray(ID));

            formView.setForm(formDescriptor);
        }
    }

    private class VisualSetupNavigator extends AbstractNavigator {
        public VisualSetupNavigator(RemoteNavigatorInterface iremoteNavigator) {
            super(iremoteNavigator);

            tree.setDropMode(DropMode.ON_OR_INSERT);
            tree.setDragEnabled(true);

            tree.rootNode.addSubTreeAction(
                    new ClientTreeAction("Создать новую форму") {
                        @Override
                        public void actionPerformed(ClientTreeActionEvent e) {
                            ClientTreeNode node = e.getNode();

                            FormDescriptor newForm = new FormDescriptor(Main.generateNewID());
                            newFormsIds.add(newForm.getID());

                            //раскрываем, чтобы загрузить узлы с сервера...
                            tree.expandPath(tree.getPathToRoot(node));

                            node.add(new NavigatorTreeNode(new NewNavigatorForm(newForm), true));

                            tree.getModel().reload(node);

                            formView.setForm(newForm);
                        }

                        @Override
                        public boolean isApplicable(TreePath path) {
                            if (path == null) {
                                return false;
                            }

                            ClientTreeNode node = (ClientTreeNode) path.getLastPathComponent();
                            if (node == null || !node.getAllowsChildren()) {
                                return false;
                            }

                            Object nodeObject = node.getUserObject();
                            return nodeObject instanceof ClientNavigatorElement;
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
                                newFormsIds.remove(formElement.form.getID());
                                Lookup.getDefault().setProperty(Lookup.DELETED_OBJECT_PROPERTY, formElement.form);
                            }
                        }
                    });
        }

        @Override
        public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
            NavigatorDescriptorView.this.openForm(element.ID);
        }

        class NewNavigatorForm extends ClientNavigatorForm implements IncrementView {
            private final FormDescriptor form;

            public NewNavigatorForm(FormDescriptor form) {
                super();
                this.form = form;
                ID = form.getID();

                IncrementDependency.add(form, "caption", this);
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
