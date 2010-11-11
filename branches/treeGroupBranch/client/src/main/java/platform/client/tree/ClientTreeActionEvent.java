package platform.client.tree;

import java.awt.event.ActionEvent;

public class ClientTreeActionEvent {

    private ClientTreeNode node;
    private ActionEvent event;

    public ClientTreeActionEvent(ClientTreeNode node) {
        this(node, null);
    }
    
    public ClientTreeActionEvent(ClientTreeNode node, ActionEvent event) {
        this.setNode(node);
        this.setEvent(event);
    }

    public ClientTreeNode getNode() {
        return node;
    }

    public void setNode(ClientTreeNode node) {
        this.node = node;
    }

    public ActionEvent getEvent() {
        return event;
    }

    public void setEvent(ActionEvent event) {
        this.event = event;
    }
}
