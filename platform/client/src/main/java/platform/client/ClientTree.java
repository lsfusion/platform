package platform.client;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class ClientTree extends JTree {

    // не вызываем верхний конструктор, потому что у JTree по умолчанию он на редкость дебильный
    public ClientTree() {
        super(new DefaultMutableTreeNode());

        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    changeCurrentElement();
                }
            }

        });

        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    changeCurrentElement();
                }
            }
        });

    }

    protected abstract void changeCurrentElement();

    public DefaultMutableTreeNode getSelectionNode() {

        TreePath path = getSelectionPath();
        if (path == null) return null;

        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

}
