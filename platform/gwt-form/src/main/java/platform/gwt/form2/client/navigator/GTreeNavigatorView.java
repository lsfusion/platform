package platform.gwt.form2.client.navigator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.window.GTreeNavigatorWindow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GTreeNavigatorView extends GNavigatorView {
    private Tree tree;
    private List<GNavigatorElement> openElements = new ArrayList<GNavigatorElement>();

    public GTreeNavigatorView(GTreeNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, new Tree(), navigatorController);
        tree = (Tree) getComponent();
        tree.setAnimationEnabled(false);
    }

    private void addNavigatorElement(TreeItem parent, GNavigatorElement element, Set<GNavigatorElement> newElements) {
        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }

        final TreeNavigatorItem node = new TreeNavigatorItem(element);
        if (parent == null) {
            tree.addItem(node);
        } else {
            parent.addItem(node);
        }
        node.setUserObject(element);

        DOM.sinkEvents(node.getElement(), Event.ONDBLCLICK);
        DOM.setEventListener(node.getElement(), new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                GNavigatorElement element = (GNavigatorElement) node.getUserObject();
                if (element.isForm) {
                    selected = element;
                    navigatorController.update();
                    navigatorController.openElement(selected);
                    event.stopPropagation();
                    event.preventDefault();
                }
            }
        });

        for (GNavigatorElement child : element.children) {
            if (newElements.contains(child)) {
                addNavigatorElement(node, child, newElements);
            }
        }
        if ("userPolicyForm".equals(element.sid) || "storeArticleForm".equals(element.sid)) {
            openNode(node);
        }

        if (openElements.contains(element)) {
            openNode(node);
        }
    }

    private void openNode(TreeItem node) {
        if (node != null) {
            node.setState(true);
            openNode(node.getParentItem());
        }
    }

    @Override
    public void refresh(Set<GNavigatorElement> newElements) {
        openElements = new ArrayList<GNavigatorElement>();

        for (Iterator<TreeItem> iterator = tree.treeItemIterator(); iterator.hasNext();) {
            TreeNavigatorItem node = (TreeNavigatorItem) iterator.next();
            if (node.getState()) {
                openElements.add(node.element);
            }
        }

        tree.clear();
        for (GNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addNavigatorElement(null, element, newElements);
            }
        }

        if (window.drawRoot && tree.getItemCount() > 0) {
            tree.getItem(0).setState(true);
        }
    }

    @Override
    public GNavigatorElement getSelectedElement() {
        return selected;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    private class TreeNavigatorItem extends TreeItem {
        public GNavigatorElement element;

        public TreeNavigatorItem(final GNavigatorElement element) {
            super(element.caption);
            this.element = element;
        }
    }
}
