package platform.client.descriptor.view;

import platform.base.context.IncrementView;
import platform.client.descriptor.FormDescriptor;
import platform.client.navigator.*;
import platform.client.tree.*;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;

class VisualSetupNavigator extends AbstractNavigator {
    private NavigatorDescriptorView navigatorDescriptorView;

    public VisualSetupNavigator(final NavigatorDescriptorView navigatorDescriptorView, RemoteNavigatorInterface iremoteNavigator) {
        super(iremoteNavigator);
        this.navigatorDescriptorView = navigatorDescriptorView;

        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setDragEnabled(true);
        tree.setCellRenderer(new VisualSetupNavigatorRenderer());

        tree.rootNode.addSubTreeAction(
                new ClientTreeAction("Создать новую форму") {
                    @Override
                    public void actionPerformed(ClientTreeActionEvent e) {
                        FormDescriptor newForm = navigatorDescriptorView.createAndOpenNewForm();

                        NavigatorTreeNode node = (NavigatorTreeNode) e.getNode();

                        //раскрываем, чтобы загрузить узлы с сервера...
                        tree.expandPath(tree.getPathToRoot(node));

                        node.addNode(new NavigatorTreeNode(tree, new NewNavigatorForm(newForm)));

                        tree.getModel().reload(node);
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
                        ClientNavigatorForm navigatorForm = (ClientNavigatorForm) e.getNode().getUserObject();
                        navigatorDescriptorView.cancelForm(navigatorForm.ID);
                    }

                    @Override
                    public boolean isApplicable(TreePath path) {
                        ClientTreeNode node = ClientTree.getNode(path);
                        if (node != null) {
                            Object nodeObject = node.getUserObject();

                            if (nodeObject instanceof ClientNavigatorForm) {
                                ClientNavigatorForm navigatorForm = (ClientNavigatorForm) nodeObject;
                                return navigatorDescriptorView.isFormChanged(navigatorForm.ID);
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
                        NavigatorTreeNode parent = (NavigatorTreeNode) e.getNode().getParent();

                        parent.removeNode(child);
                        tree.getModel().reload(parent);

                        ClientNavigatorElement navigatorElement = (ClientNavigatorElement) child.getUserObject();
                        navigatorDescriptorView.removeElement(navigatorElement.ID);
                    }

                    @Override
                    public boolean isApplicable(TreePath path) {
                        ClientTreeNode node = ClientTree.getNode(path);
                        if (node != null) {
                            Object nodeObject = node.getUserObject();

                            if (nodeObject instanceof ClientNavigatorElement) {
                                ClientNavigatorElement navigatorElement = (ClientNavigatorElement) nodeObject;
                                //не показываем для root'а
                                return navigatorElement.ID != AbstractNavigator.BASE_ELEMENT_ID;
                            }
                        }
                        return false;
                    }
                });
    }

    NavigatorTree getTree() {
        return tree;
    }

    @Override
    public void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException {
        navigatorDescriptorView.openForm(element.ID);
    }

    public void cancelNavigatorChanges() {
        cancelNodeChanges(tree.rootNode);
    }

    private void cancelNodeChanges(NavigatorTreeNode node) {
        if (node.nodeStructureChanged) {
            TreePath path = tree.getPathToRoot(node);
            boolean wasExpanded = tree.isExpanded(path);
            
            node.removeAllChildren();
            node.add(new ExpandingTreeNode());

            tree.getModel().reload(node);

            tree.collapsePath(path);
            if (wasExpanded) {
                tree.expandPath(path);
            }
            node.nodeStructureChanged = false;
        } else {
            for (int i = 0; i < node.getChildCount(); ++i) {
                TreeNode child = node.getChildAt(i);
                if (child instanceof NavigatorTreeNode) {
                    cancelNodeChanges((NavigatorTreeNode)child);
                }
            }
        }
    }

    public void nodeChanged(NavigatorTreeNode node) {
        navigatorDescriptorView.nodeChanged(node);
    }

    class VisualSetupNavigatorRenderer extends ClientTree.ClientTreeCellRenderer {

        private Color textNonSelectionColor;

        public VisualSetupNavigatorRenderer() {
            super();

            textNonSelectionColor = getTextNonSelectionColor();
        }

        @Override
        public Component getTreeCellRendererComponent(JTree iTree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            boolean changed = false;

            if (value instanceof NavigatorTreeNode) {
                NavigatorTreeNode node = (NavigatorTreeNode) value;
                if (node.nodeStructureChanged) {
                    changed = true;
                }
                
                Object userObject = node.getUserObject();
                if (userObject instanceof ClientNavigatorForm) {
                    ClientNavigatorForm form = (ClientNavigatorForm) userObject;
                    if (navigatorDescriptorView.isFormChanged(form.ID)) {
                        changed = true;
                    }
                }
            }

            setTextNonSelectionColor(changed ? Color.blue : textNonSelectionColor);

            return super.getTreeCellRendererComponent(iTree, value, sel, expanded, leaf, row, hasFocus);
        }
    }

    class NewNavigatorForm extends ClientNavigatorForm implements IncrementView {

        private final FormDescriptor form;

        public NewNavigatorForm(FormDescriptor form) {
            super();
            this.form = form;
            ID = form.getID();
            hasChildren = true;

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
