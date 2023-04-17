package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;

public abstract class WindowsController {
    private NativeSIDMap<GAbstractWindow, WindowElement> windowElementsMapping = new NativeSIDMap<>();
    public FlexWindowElement rootElement;
    public GAbstractWindow formsWindow;

    public static final String NAVBAR_TEXT_ON_HOVER = "navbar-text-on-hover";
    public static final String BACKGROUND_PREFIX = "bg-";

    public void updateElementClass(GAbstractWindow window) {
        Widget windowView = getWindowView(window);
        String elementClass = window.elementClass;

//        we still need NEED_MARGINS for formscontroller, so it's a question if it's right
//        we still need bg_ to HAS (however here we should use paddings ???) be converted to respect margins ???

        BaseImage.updateClasses(windowView, elementClass, (widget, parent, className) -> {
            if(className.equals(NAVBAR_TEXT_ON_HOVER)) {
                boolean isWindow = widget.equals(windowView);
                if((isWindow || widget instanceof FlexPanel) && parent instanceof FlexPanel) {
                    boolean thisVertical = isWindow ? window instanceof GToolbarNavigatorWindow && ((GToolbarNavigatorWindow) window).isVertical() : ((FlexPanel) widget).isVertical();
                    return ((FlexPanel) parent).isVertical() == thisVertical;
                }
            } else if(className.startsWith(BACKGROUND_PREFIX))
                return true;

            return false;
        });
    }

    public GAbstractWindow findWindowByCanonicalName(String canonicalName) {
        Result<GAbstractWindow> rWindow = new Result<>();
        windowElementsMapping.foreachKey(window -> {
            if (window.canonicalName.equals(canonicalName))
                rWindow.set(window);
        });
        return rWindow.result;
    }

    public void initializeWindows(List<GAbstractWindow> allWindows, GAbstractWindow formsWindow) {

        WindowNode rootNode = new WindowNode(0, 0, 100, 100);
        rootNode.isFlex = true;

        initializeNodes(allWindows, rootNode, formsWindow);

        if (MainFrame.verticalNavbar) {
            rootNode = rotateNavbar(rootNode);
        }
        
        rootElement = (FlexWindowElement) fillWindowChildren(rootNode);
        rootElement.initializeView(this);
        rootElement.onAddView(this);
        initNavigatorRootView(rootElement.getView());

        this.formsWindow = formsWindow;

        setDefaultVisible();

        restoreWindowsSizes();

        RootLayoutPanel.get().add(rootElement.getView());
    }

    private void initializeNodes(List<GAbstractWindow> windows, WindowNode rootNode, GAbstractWindow formsWindow) {
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
                        new WindowNode(tabChild).changeParent(tabbed);
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
                        child.changeParent(newTab);
                    }
                    newTab.changeParent(node);
                } else {
                    for (WindowNode child : newChildren) {
                        child.changeParent(node);
                    }
                }
            }
        }

        // ещё одно формирование табов: если у окна есть потомки, но с разными координатами, т.е. не в табах,
        // то это окно идёт в таб-панель. другим табом будут его потомки, сложенные в промежуточный контейнер
        for (WindowNode childNode : new ArrayList<>(rootNode.children)) {
            createTabsIfNecessary(childNode);
        }

        fillWithChildren(rootNode, rootNode.children, rootNode.x, rootNode.y, rootNode.width, rootNode.height);
        deepestFlexWindow.addChild(new WindowNode(formsWindow));
    }

    private void createTabsIfNecessary(WindowNode node) {
        if (!node.children.isEmpty() && !node.isTabbed) {
            WindowNode parent = node.parent;
            WindowNode tabWindow = new WindowNode(node.x, node.y, node.width, node.height);
            tabWindow.isTabbed = true;
            node.changeParent(tabWindow);
            WindowNode midNode = new WindowNode(node.x, node.y, node.width, node.height);
            for (WindowNode child : new ArrayList<>(node.children)) {
                child.changeParent(midNode);

                createTabsIfNecessary(child);
            }
            midNode.changeParent(tabWindow);
            tabWindow.changeParent(parent);
        }
    }

    // ported from former split/dock layout logics
    WindowNode deepestFlexWindow;
    private void fillWithChildren(WindowNode parent, List<WindowNode> leftToPut, int rectX, int rectY, int rectWidth, int rectHeight) {
        if (leftToPut.isEmpty()) {
            return;
        }

        // attach windows/splits one by one
        boolean attachedH = false;
        boolean attachedV = false;
        WindowNode attachedWindow = null;
        List<WindowNode> windowsLeft = new ArrayList<>(leftToPut);
        //searching for single full-width/height window
        for (WindowNode window : leftToPut) {
            if (windowsLeft.contains(window)) {
                attachedH = attachSingleH(window, rectX, rectWidth, rectHeight, true);
                if (!attachedH) {
                    attachedV = attachSingleV(window, rectY, rectWidth, rectHeight, true);
                }
                if (attachedH || attachedV) {
                    attachedWindow = window;
                    parent = addToParentNode(parent, window, attachedV, rectX, rectY, rectWidth, rectHeight);
                    windowsLeft.remove(window);
                    break;
                }
            }
        }

        if (!attachedH && !attachedV) {
            // if no single window found, try to find full-width/height split
            WindowNode split = putInSplit(windowsLeft, rectX, rectY, rectWidth, rectHeight, true);
            if (split == null) {
                // then try to attach split not stretched on full width/height
                split = putInSplit(windowsLeft, rectX, rectY, rectWidth, rectHeight, false);
            }
            if (split != null) {
                if (split.x == rectX && split.width == rectWidth) {
                    attachedV = attachVertically(split, rectY, rectHeight);
                } else if (split.y == rectY && split.height == rectHeight) {
                    attachedH = attachHorizontally(split, rectX, rectWidth);
                }
                if (attachedH || attachedV) {
                    attachedWindow = split;
                    parent = addToParentNode(parent, split, attachedV, rectX, rectY, rectWidth, rectHeight);

                    split.changeParent(parent);
                    for (WindowNode splitChild : split.children) {
                        windowsLeft.remove(splitChild);
                    }
                }
            }
        }

        // in the end add any window attached to the border of free space
        if (!attachedH && !attachedV) {
            for (WindowNode window : leftToPut) {
                if (windowsLeft.contains(window)) {
                    attachedH = attachSingleH(window, rectX, rectWidth, rectHeight, false);
                    if (!attachedH) {
                        attachedV = attachSingleV(window, rectY, rectWidth, rectHeight, false);
                    }
                    if (attachedH || attachedV) {
                        attachedWindow = window;
                        parent = addToParentNode(parent, window, attachedV, rectX, rectY, rectWidth, rectHeight);
                        windowsLeft.remove(window);
                        break;
                    }
                }
            }
        }

        //adjust free space
        if (attachedH) {
            rectWidth -= attachedWindow.width;
            if (attachedWindow.x == rectX) {
                rectX += attachedWindow.width;
            }
        } else if (attachedV) {
            rectHeight -= attachedWindow.height;
            if (attachedWindow.y == rectY) {
                rectY += attachedWindow.height;
            }
        }

        // search for the next window
        if (attachedH || attachedV) {
            fillWithChildren(parent, windowsLeft, rectX, rectY, rectWidth, rectHeight);
        }
    }

    private boolean attachSingleH(WindowNode window, int rectX, int rectWidth, int rectHeight, boolean fullLine) {
        if (window.height == rectHeight || !fullLine) {
            return attachHorizontally(window, rectX, rectWidth);
        }
        return false;
    }

    private boolean attachSingleV(WindowNode window, int rectY, int rectWidth, int rectHeight, boolean fullLine) {
        if (window.width == rectWidth || !fullLine) {
            return attachVertically(window, rectY, rectHeight);
        }
        return false;
    }

    private WindowNode addToParentNode(WindowNode parent, WindowNode window, boolean vertical, int rectX, int rectY, int rectWidth, int rectHeight) {
        // create new panel if direction changes
        if (parent.vertical != vertical) {
            WindowNode newParent = new WindowNode(rectX, rectY, rectWidth, rectHeight);
            newParent.isFlex = true;
            newParent.vertical = vertical;
            newParent.changeParent(parent);
            parent = newParent;
            deepestFlexWindow = parent;
        }
        window.changeParent(parent);
        return parent;
    }

    private WindowNode putInSplit(List<WindowNode> windows, int rectX, int rectY, int rectWidth, int rectHeight, boolean fullLine) {
        WindowNode flexWindow = null;
        List<WindowNode> splitChildren = null;

        // vertical
        for (int i = 0; i < windows.size() - 1; i++) {
            WindowNode first = windows.get(i);
            List<WindowNode> inOneLine = new ArrayList<>(Arrays.asList(first));
            for (int j = i + 1; j < windows.size(); j++) {
                WindowNode second = windows.get(j);
                if (first.x == second.x && first.width == second.width && first.y != second.y) {
                    inOneLine.add(second);
                }
            }
            if (inOneLine.size() > 1) {
                List<WindowNode> winds = fullLine ? findLineFilling(inOneLine, true, rectHeight) : inOneLine;
                if (winds != null) {
                    WindowNode firstWindow = winds.get(0);
                    // дополнительно проверим, что ряд окон прижат к одному из краёв
                    if (firstWindow.x == rectX || firstWindow.x + firstWindow.width == rectX + rectWidth) {
                        flexWindow = new WindowNode(firstWindow.x, rectY, firstWindow.width, rectHeight);
                        flexWindow.isFlex = true;
                        flexWindow.vertical = true;
                        splitChildren = winds;
                    }
                }
            }
        }
        // horizontal
        if (flexWindow == null) {
            for (int i = 0; i < windows.size() - 1; i++) {
                WindowNode first = windows.get(i);
                List<WindowNode> inOneLine = new ArrayList<>(Arrays.asList(first));
                for (int j = i + 1; j < windows.size(); j++) {
                    WindowNode second = windows.get(j);
                    if (first.y == second.y && first.height == second.height && first.x != second.x) {
                        inOneLine.add(second);
                    }
                }
                if (inOneLine.size() > 1) {
                    List<WindowNode> winds = fullLine ? findLineFilling(inOneLine, false, rectWidth) : inOneLine;
                    if (winds != null) {
                        WindowNode firstWindow = winds.get(0);
                        if (firstWindow.y == rectY || firstWindow.y + firstWindow.height == rectY + rectHeight) {
                            flexWindow = new WindowNode(rectX, firstWindow.y, rectWidth, firstWindow.height);
                            flexWindow.isFlex = true;
                            flexWindow.vertical = false;
                            splitChildren = winds;
                        }
                    }
                }
            }
        }

        if (splitChildren != null) {
            for (WindowNode window : splitChildren) {
                window.changeParent(flexWindow);
            }
        }

        return flexWindow;
    }

    private List<WindowNode> findLineFilling(List<WindowNode> windows, boolean vertical, int dimension) {
        // здесь подразумевается, что все найденные окна одной ширины/высоты, расположены в один ряд без наложений и пробелов
        int sum = 0;
        List<WindowNode> result = new ArrayList<>();

        for (WindowNode window : windows) {
            sum += vertical ? window.height : window.width;
            result.add(window);
        }
        return sum == dimension ? result : null;
    }

    private boolean attachHorizontally(WindowNode window, int rectX, int rectWidth) {
        return window.x == rectX || window.x + window.width == rectX + rectWidth;
    }

    private boolean attachVertically(WindowNode window, int rectY, int rectHeight) {
        return window.y == rectY || window.y + window.height == rectY + rectHeight;
    }
    
    // rotates navbar and returns new root node
    private WindowNode rotateNavbar(WindowNode rootNode) {
        WindowNode navbarNode = findNavbarWindow(rootNode);
        assert navbarNode != null;

        navbarNode.vertical = true;
        navbarNode.x = -1; // to be sure it goes first (left), as children are ordered by x/y coordinates

        // change children orientation
        for (WindowNode child : navbarNode.children) {
            child.y = child.x;
            child.x = 0;
            int tmp = child.width;
            child.width = child.height;
            child.height = tmp;
        }

        // create new horizontal root flex panel
        WindowNode newRootNode = new WindowNode(0, 0, 100, 100);
        newRootNode.isFlex = true;
        newRootNode.vertical = false;
        navbarNode.changeParent(newRootNode);
        rootNode.changeParent(newRootNode);
        
        return newRootNode;
    }
    
    private WindowNode findNavbarWindow(WindowNode parent) {
        for (WindowNode child : parent.children) {
            if (child.window instanceof GNavigatorWindow && ((GNavigatorWindow) child.window).isRoot()) {
                return parent;
            } else {
                WindowNode maybeRoot = findNavbarWindow(child);
                if (maybeRoot != null) {
                    return maybeRoot;
                }
            }
        }
        return null;
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
            assert node.isFlex;
            return new FlexWindowElement(node.vertical, this, node.x, node.y, node.width, node.height);
        }
    }

    public void registerMobileWindow(GAbstractWindow window) {
        registerWindow(window, null);
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

        updatePanels();
    }

    private void updatePanels() {
        FlexPanel.updatePanels(rootElement.getView());

        RootLayoutPanel.get().onResize();
    }

    public void resetLayout() {
        setDefaultVisible();
        rootElement.resetWindowSize();

        updatePanels();
    }

    private void setDefaultVisible() {
        windowElementsMapping.foreachEntry((window, windowElement) -> {
            if (windowElement != null) {
                windowElement.setVisible(window.visible);
            }
        });
    }

    // in current layout formsWindow is always added as the CENTER widget.
    // so to enable full screen mode we only need to hide all non-CENTER windows in root split panel
    private Widget formsWidget;
    public void setFullScreenMode(boolean fullScreen) {
        ResizableSimplePanel formsPanel = (ResizableSimplePanel) getWindowView(formsWindow);
        if(fullScreen) {
            RootLayoutPanel.get().remove(rootElement.getView());
            formsWidget = formsPanel.getWidget();
            RootLayoutPanel.get().add(formsWidget);
        } else {
            formsPanel.setWidget(formsWidget);
            formsWidget = null;
            RootLayoutPanel.get().add(rootElement.getView());
        }
    }

    public abstract Widget getWindowView(GAbstractWindow window);

    public void storeWindowsSizes() {
        Storage storage = Storage.getLocalStorageIfSupported();
        if (storage != null && rootElement != null) {
            rootElement.storeWindowsSizes(storage);
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

    public void initNavigatorRootView(Widget navRootWidget) {
//        if(MainFrame.useBootstrap)
//            navRootWidget.addStyleName("bg-body-secondary");
    }

    private class WindowNode {
        private GAbstractWindow window;
        private WindowNode parent;
        private List<WindowNode> children = new ArrayList<>();

        public boolean isTabbed = false;
        public boolean isFlex = false;
        public boolean vertical = true;

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

        public void changeParent(WindowNode parent) {
            if (this.parent != null) {
                this.parent.removeChild(this);
            }
            this.parent = parent;
            parent.addChild(this);
        }
    }
}
