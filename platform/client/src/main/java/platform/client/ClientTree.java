package platform.client;

import platform.client.descriptor.nodes.ClientTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.*;

public abstract class ClientTree extends JTree {

    // не вызываем верхний конструктор, потому что у JTree по умолчанию он на редкость дебильный
    public ClientTree() {
        super(new DefaultMutableTreeNode());

        addMouseListener(new PopupTrigger());

        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                   // changeCurrentElement();
                }
            }
        });

    }

    //protected abstract void changeCurrentElement();

    public DefaultMutableTreeNode getSelectionNode() {

        TreePath path = getSelectionPath();
        if (path == null) return null;

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    class PopupTrigger extends MouseAdapter {
        public void mouseReleased(MouseEvent e) {

            if (e.getClickCount() == 2) {
               // changeCurrentElement();
            }
            if (e.isPopupTrigger() || e.getClickCount() == 2) {
                int x = e.getX();
                int y = e.getY();
                TreePath path = getPathForLocation(x, y);
                if (path != null) {
                    setSelectionPath(path);
                    JPopupMenu popup = new JPopupMenu();
                    Action defaultAction = null;
                    int cnt = path.getPathCount();
                    for (int i = 0; i < cnt; i++) {
                        Object oNode = path.getPathComponent(i);
                        if (oNode instanceof ClientTreeNode) {
                            ClientTreeNode node = (ClientTreeNode) oNode;

                            for (Action act : node.subTreeActions) {
                                popup.add(act);
                                if (defaultAction == null) {
                                    defaultAction = act;
                                }
                            }

                            if (i == cnt - 2) {
                                for (Action act : node.sonActions) {
                                    popup.add(act);
                                    if (defaultAction == null) {
                                        defaultAction = act;
                                    }
                                }
                            }

                            if (i == cnt - 1) {
                                for (Action act : node.nodeActions) {
                                    popup.add(act);
                                    if (defaultAction == null) {
                                        defaultAction = act;
                                    }
                                }
                            }
                        }
                    }

                    if (e.isPopupTrigger() && popup.getComponentCount() > 0) {
                        popup.show(ClientTree.this, x, y);
                    } else if (e.getClickCount() == 2 && defaultAction != null) {
                        defaultAction.actionPerformed((ActionEvent) null);
                    }
                }
            }
        }
    }

    class CloseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = getSelectionNode();
            node.removeFromParent();
        }
    }


}
