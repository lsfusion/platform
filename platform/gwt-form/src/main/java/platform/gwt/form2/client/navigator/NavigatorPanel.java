package platform.gwt.form2.client.navigator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.form2.client.dispatch.NavigatorDispatchAsync;
import platform.gwt.form2.client.events.OpenFormEvent;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElements;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElementsResult;
import platform.gwt.form2.shared.view.GNavigatorElement;

public class NavigatorPanel extends ScrollPanel {
    private final NavigatorDispatchAsync dispatcher = new NavigatorDispatchAsync();

    private Tree tree;

    public NavigatorPanel() {
        createTreeGrid();

        add(tree);
    }

    private void createTreeGrid() {
        tree = new Tree();
        tree.setAnimationEnabled(false);

        dispatcher.execute(new GetNavigatorElements(), new ErrorAsyncCallback<GetNavigatorElementsResult>() {
            @Override
            public void success(GetNavigatorElementsResult result) {
                addNavigatorElement(null, result.root);
            }

            private void addNavigatorElement(TreeItem parent, GNavigatorElement element) {
                final TreeItem node = parent == null ? tree.addItem(element.caption) : parent.addItem(element.caption);
                node.setUserObject(element);

                DOM.sinkEvents(node.getElement(), Event.ONDBLCLICK);
                DOM.setEventListener(node.getElement(), new EventListener() {
                    @Override
                    public void onBrowserEvent(Event event) {
                        GNavigatorElement element = (GNavigatorElement) node.getUserObject();
                        if (element.isForm) {
                            OpenFormEvent.fireEvent(element.sid, element.caption);
                            event.stopPropagation();
                            event.preventDefault();
                        }
                    }
                });

                for (GNavigatorElement child : element.children) {
                    addNavigatorElement(node, child);
                }
                if ("userPolicyForm".equals(element.sid) || "storeArticleForm".equals(element.sid)) {
                    openNode(node);
                }
            }

            private void openNode(TreeItem node) {
                if (node != null) {
                    node.setState(true);
                    openNode(node.getParentItem());
                }
            }
        });
    }
}
