package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.navigator.view.GNavigatorView;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;

public abstract class WindowsController extends CustomSplitLayoutPanel {
    private Map<GAbstractWindow, WindowElement> windowElementsMapping = new HashMap<>();
    private SplitWindowElement rootElement;

    public void initializeWindows(List<GAbstractWindow> allWindows, GAbstractWindow formsWindow) {

        WindowNode rootNode = new WindowNode(0, 0, 100, 100);

        initializeNodes(allWindows, rootNode);

        rootElement = (SplitWindowElement) fillWindowChildren(rootNode);
        rootElement.addElement(new SimpleWindowElement(this, formsWindow, -1, -1, -1, -1));

        Widget rootView = rootElement.initializeView();

        setDefaultVisible();

        // don't restore windows sizes on style change as we need to auto adjust sizes   
        if (restoreBootstrapStyle() == MainFrame.useBootstrap) {
            restoreWindowsSizes();
        }

        RootLayoutPanel.get().add(rootView);
    }

    private void initializeNodes(List<GAbstractWindow> windows, WindowNode rootNode) {
        List<WindowNode> nodes = new ArrayList<>();

        // все окна с одинаковыми координатами складываем в табы
        Set<GAbstractWindow> allInTabs = new HashSet<>();
        for (int i = 0; i < windows.size(); i++) {
            GAbstractWindow first = windows.get(i);
            Set<GAbstractWindow> tabChildren = null;

            if (!allInTabs.contains(first)) {
                for (int j = i + 1; j < windows.size(); j++) {
                    GAbstractWindow second = windows.get(j);
                    if (first.x == second.x && first.y == second.y && first.width == second.width && first.height == second.height) {
                        if (tabChildren == null) {
                            tabChildren = new LinkedHashSet<>();
                            tabChildren.add(first);
                            allInTabs.add(first);
                        }
                        tabChildren.add(second);
                        allInTabs.add(second);
                    }
                }

                if (tabChildren != null) {
                    WindowNode tabbed = new WindowNode(first.x, first.y, first.width, first.height);
                    tabbed.isTabbed = true;
                    for (GAbstractWindow tabChild : tabChildren) {
                        new WindowNode(tabChild).setParent(tabbed);
                    }
                    nodes.add(tabbed);
                } else {
                    nodes.add(new WindowNode(first));
                }
            }
        }

        nodes.add(rootNode);

        // устанавливаем родительские связи
        for (int i = 0; i < nodes.size(); i++) {
            WindowNode node = nodes.get(i);
            List<WindowNode> newChildren = new ArrayList<>();
            for (int j = 0; j < nodes.size(); j++) {
                if (i != j) {
                    WindowNode node2 = nodes.get(j);
                    if (node.includes(node2)) {
                        if (node2.parent != null) {
                            if (node2.parent.includes(node)) {
                                node2.parent.removeChild(node2);
                                newChildren.add(node2);
                            }
                        } else {
                            newChildren.add(node2);
                        }
                    }
                }
            }

            // если на табы накладываются другие окна, складываем их в одно промежуточное окно и добавляем в виде ещё одной вкладки
            if (!newChildren.isEmpty()) {
                if (node.isTabbed) {
                    WindowNode newTab = new WindowNode(node.x, node.y, node.width, node.height);
                    for (WindowNode child : newChildren) {
                        child.setParent(newTab);
                    }
                    newTab.setParent(node);
                } else {
                    for (WindowNode child : newChildren) {
                        child.setParent(node);
                    }
                }
            }
        }

        // ещё одно формирование табов: если у окна есть потомки, но с разными координатами, т.е. не в табах,
        // то это окно идёт в таб-панель. другим табом будут его потомки, сложенные в промежуточный контейнер
        for (WindowNode childNode : new ArrayList<>(rootNode.children)) {
            createTabsIfNecessary(childNode);
        }
    }

    private void createTabsIfNecessary(WindowNode node) {
        if (!node.children.isEmpty() && !node.isTabbed) {
            WindowNode parent = node.parent;
            WindowNode tabWindow = new WindowNode(node.x, node.y, node.width, node.height);
            tabWindow.isTabbed = true;
            parent.removeChild(node);
            node.setParent(tabWindow);
            WindowNode midNode = new WindowNode(node.x, node.y, node.width, node.height);
            for (WindowNode child : new ArrayList<>(node.children)) {
                node.removeChild(child);
                child.setParent(midNode);

                createTabsIfNecessary(child);
            }
            midNode.setParent(tabWindow);
            tabWindow.setParent(parent);
        }
    }

    private WindowElement fillWindowChildren(WindowNode node) {
        WindowElement nodeElement = createElementForNode(node);
        for (WindowNode child : node.children) {
            nodeElement.addElement(fillWindowChildren(child));
        }

        return nodeElement;
    }

    private WindowElement createElementForNode(WindowNode node) {
        if (node.isTabbed) {
            return new TabbedWindowElement(this, node.x, node.y, node.width, node.height);
        } else if (node.children.isEmpty()) {
            return new SimpleWindowElement(this, node.window, node.x, node.y, node.width, node.height);
        } else {
            return new SplitWindowElement(this, node.x, node.y, node.width, node.height);
        }
    }

    public void registerWindow(GAbstractWindow window, WindowElement windowElement) {
        windowElementsMapping.put(window, windowElement);
    }

    public void updateVisibility(Map<GAbstractWindow, Boolean> windows) {
        for (Map.Entry<GAbstractWindow, Boolean> entry : windows.entrySet()) {
            WindowElement windowElement = windowElementsMapping.get(entry.getKey());
            if (windowElement != null) {
                windowElement.setVisible(entry.getValue());
            }
        }
    }
    
    public void resetLayout() {
        setDefaultVisible();
        rootElement.resetWindowSize();
        autoSizeWindows();
    }

    private void setDefaultVisible() {
        for (GAbstractWindow window : windowElementsMapping.keySet()) {
            WindowElement windowElement = windowElementsMapping.get(window);
            if (windowElement != null) {
                windowElement.setVisible(window.visible);
            }
        }
    }

    public void autoSizeWindows() {
        // we have to wait until windows are really visible and filled
        // as setting calculated or auto sizes are in one method and auto sizing needs windows to be drawn
        if (rootElement.getView().getOffsetWidth() > 0) {
            rootElement.autoSizeWindows();
        }
    }

    // in current layout formsWindow is always added as the CENTER widget.
    // so to enable full screen mode we only need to hide all non-CENTER windows in root split panel
    public void setFullScreenMode(boolean fullScreen) {
        rootElement.setBorderWindowsHidden(fullScreen);
    }

    public abstract Widget getWindowView(GAbstractWindow window);
    public abstract GNavigatorView getNavigatorView(GNavigatorWindow window);

    public void storeWindowsSizes() {
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage != null) {
            if (rootElement != null) {
                rootElement.storeWindowsSizes(storage);
            }
        }
    }
    
    public void restoreWindowsSizes() {
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage != null) {
            rootElement.restoreWindowsSizes(storage);
        }
    }

    public void storeEditMode() {
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage != null) {
            storage.setItem("editMode", String.valueOf(FormsController.getEditModeIndex()));
        }
    }

    public int restoreEditMode() {
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage != null) {
            String editMode = storage.getItem("editMode");
            if(editMode != null) {
                return Integer.parseInt(editMode);
            }
        }
        return 0;
    }

    public void storeBootstrapStyle() {
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage != null) {
            storage.setItem("bootstrapStyle", String.valueOf(MainFrame.useBootstrap));
        }
    }

    public boolean restoreBootstrapStyle() {
        Storage storage = Storage.getLocalStorageIfSupported();
        return storage != null && Boolean.parseBoolean(storage.getItem("bootstrapStyle"));
    }

    private class WindowNode {
        private GAbstractWindow window;
        private WindowNode parent;
        private List<WindowNode> children = new ArrayList<>();

        public boolean isTabbed = false;

        private int x;
        private int y;
        private int width;
        private int height;

        public WindowNode(GAbstractWindow window) { // separate windows
            this(window.x, window.y, window.width, window.height);
            this.window = window;
        }

        public WindowNode(int x, int y, int width, int height) { // for those in tabs (+root)
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void addChild(WindowNode child) {
            children.add(child);
        }

        public void removeChild(WindowNode child) {
            children.remove(child);
        }

        public boolean includes(WindowNode node) {
            return x <= node.x && y <= node.y && x + width >= node.x + node.width && y + height >= node.y + node.height;
        }

        public void setParent(WindowNode parent) {
            this.parent = parent;
            parent.addChild(this);
        }
    }
}
