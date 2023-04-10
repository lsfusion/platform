package lsfusion.gwt.client.navigator.view;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GTreeNavigatorWindow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public class GTreeNavigatorView extends GNavigatorView<GTreeNavigatorWindow> {
    private NavigatorTree tree;
    private List<GNavigatorElement> openElements = new ArrayList<>();

    public GTreeNavigatorView(GTreeNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);
        setComponent(new NavigatorTree());
        tree = (NavigatorTree) getComponent();
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

        for (GNavigatorElement child : element.children) {
            if (newElements.contains(child)) {
                addNavigatorElement(node, child, newElements);
            }
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
        openElements = new ArrayList<>();

        for (Iterator<TreeItem> iterator = tree.treeItemIterator(); iterator.hasNext();) {
            TreeNavigatorItem node = (TreeNavigatorItem) iterator.next();
            if (node.getState()) {
                openElements.add(node.element);
            }
        }

        tree.clear();
        for (GNavigatorElement element : newElements) {
            if (!newElements.contains(element.parent)) {
                addNavigatorElement(null, element, newElements);
            }
        }
    }

    private void doubleClicked(Event event) {
        TreeNavigatorItem selectedItem = (TreeNavigatorItem) tree.getSelectedItem();
        if (selectedItem != null)
            selectElement(selectedItem.element, event);
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
            super(SafeHtmlUtils.fromString(element.caption));
            this.element = element;
            setUserObject(element);
        }
    }

    private class NavigatorTree extends Tree {
        public NavigatorTree() {
            super();
            sinkEvents(Event.ONDBLCLICK);
        }

        @Override
        public void onBrowserEvent(Event event) {
            if (event.getTypeInt() == Event.ONDBLCLICK) {
                stopPropagation(event);
                doubleClicked(event);
            }
            super.onBrowserEvent(event);
        }
    }
}
