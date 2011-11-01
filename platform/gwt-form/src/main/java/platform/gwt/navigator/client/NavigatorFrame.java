package platform.gwt.navigator.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;
import platform.gwt.base.client.BaseFrame;
import platform.gwt.base.client.ui.ToolStripPanel;
import platform.gwt.navigator.shared.actions.GetNavigatorElements;
import platform.gwt.navigator.shared.actions.GetNavigatorElementsResult;
import platform.gwt.utils.GwtUtils;
import platform.gwt.view.GNavigatorElement;

public class NavigatorFrame extends VLayout implements EntryPoint {
    private static final NavigatorFrameMessages messages = NavigatorFrameMessages.Instance.get();
    private final static StandardDispatchAsync navigatorService = new StandardDispatchAsync(new DefaultExceptionHandler());

    public void onModuleLoad() {
        setWidth100();
        setHeight100();

        HLayout main = new HLayout(20);
        main.setWidth100();
        main.setHeight100();
        main.addMember(createTreeGrid());

        addMember(new ToolStripPanel(messages.title()));
        addMember(main);

        draw();

        GwtUtils.removeLoaderFromHostedPage();
    }

    private TreeGrid createTreeGrid() {
        final TreeGrid navigatorTreeGrid = new TreeGrid();
        navigatorTreeGrid.setWidth("300");
        navigatorTreeGrid.setHeight100();
        navigatorTreeGrid.setSelectionType(SelectionStyle.SINGLE);
        navigatorTreeGrid.setShowRollOver(false);
        navigatorTreeGrid.setCanResizeFields(true);
        navigatorTreeGrid.setCanSort(false);
        navigatorTreeGrid.setCanEdit(false);
        navigatorTreeGrid.setShowHeaderContextMenu(false);
        navigatorTreeGrid.setShowHeaderMenuButton(false);
        navigatorTreeGrid.setShowOpenIcons(false);
        navigatorTreeGrid.setShowDropIcons(false);
        navigatorTreeGrid.setShowResizeBar(true);

        navigatorTreeGrid.addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
            @Override
            public void onRecordDoubleClick(RecordDoubleClickEvent event) {
                boolean isForm = Boolean.parseBoolean(event.getRecord().getAttribute("isForm"));
                if (isForm) {
                    String sid = event.getRecord().getAttribute("sid");
                    Window.open(BaseFrame.getPageUrlPreservingParameters("form.jsp", "formSID", sid), "", "");
                }
            }
        });


        navigatorService.execute(new GetNavigatorElements(), new AsyncCallback<GetNavigatorElementsResult>() {
            @Override
            public void onFailure(Throwable t) {
                GWT.log("Ошибка во время чтения данных с сервера: ", t);
                SC.warn("Ошибка во время чтения данных с сервера: <br/>" + t.getMessage());
            }

            @Override
            public void onSuccess(GetNavigatorElementsResult result) {
                Tree dataTree = createTree(result.root);
                navigatorTreeGrid.setData(dataTree);
            }
        });

        return navigatorTreeGrid;
    }

    private Tree createTree(GNavigatorElement root) {
        Tree dataTree = new Tree();
        dataTree.setIdField("key");
        dataTree.setParentIdField("parent");
        dataTree.setTitleProperty("caption");
        dataTree.setNameProperty("sid");

        TreeNode rootNode = getNode(0, root);

        dataTree.setRoot(rootNode);

        return dataTree;
    }

    private TreeNode getNode(int parentId, GNavigatorElement currentElement) {
        int currentKey = getNextKey();

        TreeNode currentNode = new TreeNode();
        currentNode.setAttribute("caption", currentElement.caption);
        currentNode.setAttribute("sid", currentElement.sid);
        currentNode.setAttribute("icon", currentElement.icon);
        currentNode.setAttribute("isForm", currentElement.isForm);
        currentNode.setAttribute("key", currentKey);
        currentNode.setAttribute("parent", parentId);

        TreeNode[] children = new TreeNode[currentElement.children.size()];
        int i = 0;
        for (GNavigatorElement child : currentElement.children) {
            children[i++] = getNode(currentKey, child);
        }

        currentNode.setAttribute("children", children);

        return currentNode;
    }

    private int nextKey = 1;

    private int getNextKey() {
        return nextKey++;
    }
}
