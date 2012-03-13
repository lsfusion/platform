package platform.client.descriptor.view;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.navigator.*;
import platform.client.tree.*;
import platform.interop.Constants;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.Enumeration;

import static platform.client.navigator.ClientNavigatorElement.BASE_ELEMENT_SID;

class VisualSetupNavigatorPanel extends AbstractNavigatorPanel {
    private NavigatorDescriptorView navigatorDescriptorView;

    public VisualSetupNavigatorPanel(final NavigatorDescriptorView navigatorDescriptorView, ClientNavigator clientNavigator) {
        super(clientNavigator);
        this.navigatorDescriptorView = navigatorDescriptorView;

        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setDragEnabled(true);
        tree.setCellRenderer(new VisualSetupNavigatorRenderer());

        tree.rootNode.addSubTreeAction(
                new ClientTreeAction(ClientResourceBundle.getString("descriptor.view.create.new.form")) {
                    @Override
                    public void actionPerformed(ClientTreeActionEvent e) {
                        FormDescriptor newForm = navigatorDescriptorView.createAndOpenNewForm();

                        NavigatorTreeNode node = (NavigatorTreeNode) e.getNode();

                        //раскрываем, чтобы загрузить узлы с сервера...
                        tree.expandPath(tree.getPathToRoot(node));

                        node.addNode(new NavigatorTreeNode(tree, new ClientNavigatorForm(newForm.getID(), Constants.getDefaultFormSID(newForm.getID()), newForm.getCaption())));

                        tree.getModel().reload(node);
                    }
                });

        tree.rootNode.addSubTreeAction(
                new ClientTreeAction(ClientResourceBundle.getString("descriptor.view.create.create.new.element")) {
                    @Override
                    public void actionPerformed(ClientTreeActionEvent e) {
                        String caption = JOptionPane.showInputDialog(null, ClientResourceBundle.getString("descriptor.view.create.enter.element.name"), ClientResourceBundle.getString("descriptor.view.create.new.element"), JOptionPane.QUESTION_MESSAGE);

                        if (caption != null) {
                            ClientNavigatorElement newElement = navigatorDescriptorView.createNewNavigatorElement(caption);

                            NavigatorTreeNode node = (NavigatorTreeNode) e.getNode();

                            //раскрываем, чтобы загрузить узлы с сервера...
                            tree.expandPath(tree.getPathToRoot(node));

                            node.addNode(new NavigatorTreeNode(tree, newElement));

                            tree.getModel().reload(node);
                        }
                    }
                });

        tree.rootNode.addSubTreeAction(
                new ClientTreeAction(ClientResourceBundle.getString("descriptor.view.cancel.changes")) {
                    @Override
                    public void actionPerformed(ClientTreeActionEvent e) {
                        ClientNavigatorForm navigatorForm = (ClientNavigatorForm) e.getNode().getUserObject();
                        navigatorDescriptorView.cancelForm(navigatorForm.getSID());
                    }

                    @Override
                    public boolean isApplicable(ClientTreeNode node) {
                        Object nodeObject = node.getUserObject();

                        if (nodeObject instanceof ClientNavigatorForm) {
                            ClientNavigatorForm navigatorForm = (ClientNavigatorForm) nodeObject;
                            return navigatorDescriptorView.isFormChanged(navigatorForm.getSID());
                        }
                        return false;
                    }
                });

        tree.rootNode.addSubTreeAction(
                new ClientTreeAction(ClientResourceBundle.getString("descriptor.view.delete")) {
                    @Override
                    public void actionPerformed(ClientTreeActionEvent e) {
                        ClientTreeNode child = e.getNode();
                        NavigatorTreeNode parent = (NavigatorTreeNode) child.getParent();

                        Enumeration<ClientTreeNode> nodes = child.depthFirstEnumeration();
                        while (nodes.hasMoreElements()) {
                            ClientTreeNode node = nodes.nextElement();
                            if (node instanceof NavigatorTreeNode) {
                                NavigatorTreeNode navigatorNode = (NavigatorTreeNode) node;
                                navigatorDescriptorView.removeElement(navigatorNode.navigatorElement.getSID());
                            }
                        }

                        parent.removeNode(child);
                        tree.getModel().reload(parent);
                    }

                    @Override
                    public boolean isApplicable(ClientTreeNode node) {
                        Object nodeObject = node.getUserObject();

                        if (nodeObject instanceof ClientNavigatorElement) {
                            ClientNavigatorElement navigatorElement = (ClientNavigatorElement) nodeObject;
                            //не показываем для root'а
                            return !navigatorElement.getSID().equals(BASE_ELEMENT_SID);
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
        navigatorDescriptorView.openForm(element.getSID());
    }

    @Override
    public void openAction(ClientNavigatorAction action) {
        //todo: редактирование action'а
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
                    cancelNodeChanges((NavigatorTreeNode) child);
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
                    if (navigatorDescriptorView.isFormChanged(form.getSID())) {
                        changed = true;
                    }
                }
            }

            setTextNonSelectionColor(changed ? Color.blue : textNonSelectionColor);

            return super.getTreeCellRendererComponent(iTree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
}
