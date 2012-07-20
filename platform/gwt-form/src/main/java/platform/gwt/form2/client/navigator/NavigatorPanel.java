package platform.gwt.form2.client.navigator;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.form2.client.dispatch.FormDispatchAsync;
import platform.gwt.form2.client.events.OpenFormEvent;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElements;
import platform.gwt.form2.shared.actions.navigator.GetNavigatorElementsResult;
import platform.gwt.view2.GNavigatorElement;

public class NavigatorPanel extends ScrollPanel {
    private final FormDispatchAsync dispatcher = new FormDispatchAsync(new DefaultExceptionHandler());

    private Tree tree;


    public NavigatorPanel() {
        createTreeGrid();

        add(tree);
    }

    private void createTreeGrid() {
        tree = new Tree();
        tree.setAnimationEnabled(false);

        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
                TreeItem item = event.getSelectedItem();
                GNavigatorElement element = (GNavigatorElement) item.getUserObject();
                if (element.isForm) {
                    OpenFormEvent.fireEvent(element.sid, element.caption);
                }
            }
        });

        dispatcher.execute(new GetNavigatorElements(), new ErrorAsyncCallback<GetNavigatorElementsResult>() {
            @Override
            public void success(GetNavigatorElementsResult result) {
                addNavigatorElement(null, result.root);
            }

            private void addNavigatorElement(TreeItem parent, GNavigatorElement element) {
                TreeItem node = parent == null ? tree.addItem(element.caption) : parent.addItem(element.caption);
                node.setUserObject(element);
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
