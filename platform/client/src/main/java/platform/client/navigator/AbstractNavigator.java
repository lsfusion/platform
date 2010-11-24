package platform.client.navigator;

import platform.client.logics.DeSerializer;
import platform.interop.navigator.RemoteNavigatorInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public abstract class AbstractNavigator extends JPanel {
    public final RemoteNavigatorInterface remoteNavigator;

    protected final NavigatorTree tree;
    public static final int BASE_ELEMENT_ID = 0;

    public AbstractNavigator(RemoteNavigatorInterface iremoteNavigator) {

        remoteNavigator = iremoteNavigator;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(175, 400));

        tree = new NavigatorTree(this);
        
        JScrollPane pane = new JScrollPane(tree);
        add(pane);
    }

    public abstract void openForm(ClientNavigatorForm element) throws IOException, ClassNotFoundException;

    protected java.util.List<ClientNavigatorElement> getNodeElements(int elementID) throws IOException {
        return DeSerializer.deserializeListClientNavigatorElement(
                                                remoteNavigator.getElementsByteArray(elementID));
    }

    public void nodeChanged(NavigatorTreeNode node) {
        //do nothing
    }
}
