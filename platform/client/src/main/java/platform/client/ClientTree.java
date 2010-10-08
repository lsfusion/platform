package platform.client;

import platform.client.descriptor.nodes.ClientTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.*;
import java.util.ArrayList;

public class ClientTree extends JTree {


    // не вызываем верхний конструктор, потому что у JTree по умолчанию он на редкость дебильный
    public ClientTree() {
        super(new DefaultMutableTreeNode());
        setToggleClickCount(-1);
        addMouseListener(new PopupTrigger());

        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    TreePath path = getSelectionPath();
                    ArrayList<Action> list = getActions(path);
                    if (list.size() > 0) {
                        list.get(0).actionPerformed(null);
                    }
                }
            }
        });

    }

    protected void changeCurrentElement() {        
    }

    public DefaultMutableTreeNode getSelectionNode() {

        TreePath path = getSelectionPath();
        if (path == null) return null;

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    class PopupTrigger extends MouseAdapter {
        public void mouseReleased(MouseEvent e) {

            if (e.isPopupTrigger() || e.getClickCount() == 2) {
                int x = e.getX();
                int y = e.getY();
                TreePath path = getPathForLocation(x, y);
                if (path != null) {
                    setSelectionPath(path);
                    JPopupMenu popup = new JPopupMenu();
                    ArrayList<Action> list = getActions(path);
                    Action defaultAction = null;
                    if (list.size() > 0) {
                        defaultAction = list.get(0);
                    }
                    for (Action act : list) {
                        popup.add(act);
                    }

                    if (e.isPopupTrigger() && popup.getComponentCount() > 0) {
                        popup.show(ClientTree.this, x, y);
                    } else if (e.getClickCount() == 2 && defaultAction != null) {
                        defaultAction.actionPerformed(null);
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

    private ArrayList<Action> getActions(TreePath path) {
        if (path == null) {
            return null;
        }
        ArrayList<Action> list = new ArrayList<Action>();

        int cnt = path.getPathCount();
        for (int i = 0; i < cnt; i++) {
            Object oNode = path.getPathComponent(i);
            if (oNode instanceof ClientTreeNode) {
                ClientTreeNode node = (ClientTreeNode) oNode;

                for (Action act : node.subTreeActions) {
                    list.add(act);
                }

                if (i == cnt - 2) {
                    for (Action act : node.sonActions) {
                        list.add(act);
                    }
                }

                if (i == cnt - 1) {
                    for (Action act : node.nodeActions) {
                        list.add(act);
                    }
                }
            }
        }
        return list;
    }
}
