package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.*;

import static com.google.gwt.user.client.ui.DockLayoutPanel.Direction.*;

public class SplitWindowElement extends WindowElement {
    private final static String SIMPLE_WINDOW_CHILD_STYLE = "split-simple-window";
    
    // with visibility
    private HashMap<WindowElement, Boolean> children = new HashMap<>();

    // храним вместе с порядком и направлением, чтобы знать, куда вставлять элемент при его видимости
    protected LinkedHashMap<WindowElement, DockLayoutPanel.Direction> windowDirections = new LinkedHashMap<>();

    private CustomSplitLayoutPanel splitPanel = new CustomSplitLayoutPanel(6); // with borders

    public SplitWindowElement(WindowsController main, int x, int y, int width, int height) {
        super(main, x, y, width, height);
    }

    @Override
    public void addElement(WindowElement element) {
        if (!children.containsKey(element)) {
            children.put(element, true);
        }
        element.parent = this;
    }

    @Override
    public String getCaption() {
        String caption = "";
        for (Iterator<WindowElement> iterator = children.keySet().iterator(); iterator.hasNext(); ) {
            String childCaption = iterator.next().getCaption();
            if (childCaption != null) {
                caption += childCaption;
                if (iterator.hasNext()) {
                    caption += "/";
                }
            }
        }
        return caption.isEmpty() ? null : caption;
    }

    @Override
    public Widget initializeView() {
        // рекурсивно начинаем заполнять пространство окна
        fillWithChildren(new ArrayList<>(children.keySet()), x, y, width, height);
        return super.initializeView();
    }
    
    private void fillWithChildren(List<WindowElement> leftToPut, int rectX, int rectY, int rectWidth, int rectHeight) {
        if (leftToPut.isEmpty()) {
            return;
        }

        List<WindowElement> windowsLeft = new ArrayList<>(leftToPut);
        for (WindowElement window : leftToPut) {
            if (windowsLeft.size() == 1) {
                Widget view = windowsLeft.get(0).initializeView();
                splitPanel.add(view);
                addStyleClasses(window);
                windowDirections.put(windowsLeft.get(0), CENTER);
                return;
            }  

            boolean attached = false;
            if (windowsLeft.contains(window)) {
                if (window.height == rectHeight) {
                    attached = attachHorizontally(window, rectX, rectWidth);
                    if (attached) {
                        rectWidth -= window.width;
                        if (window.x == rectX) {
                            rectX += window.width;
                        }
                    }
                } else if (window.width == rectWidth) {
                    attached = attachVertically(window, rectY, rectHeight);
                    if (attached) {
                        rectHeight -= window.height;
                        if (window.y == rectY) {
                            rectY += window.height;
                        }
                    }
                }
                if (attached) {
                    windowsLeft.remove(window);
                }
            }
        }
        
        if (windowsLeft.size() < leftToPut.size()) {
            // какое-то окно нашло своё место - делаем ещё круг
            fillWithChildren(windowsLeft, rectX, rectY, rectWidth, rectHeight);
        } else {
            // иначе пробуем сложить часть окон в гор.или верт. сплит
            SplitWindowElement split = putInSplit(windowsLeft, rectX, rectY, rectWidth, rectHeight); 
            if (split != null) {
                addElement(split);
                
                for (WindowElement splitChild : split.children.keySet()) {
                    children.remove(splitChild);
                    windowsLeft.remove(splitChild);
                }
                
                if (split.x == rectX && split.width == rectWidth) {
                    boolean attached = attachVertically(split, rectY, rectHeight);
                    if (attached) {
                        rectHeight -= split.height;
                        if (split.y == rectY) {
                            rectY += split.height;
                        }
                    }
                } else if (split.y == rectY && split.height == rectHeight) {
                    boolean attached = attachHorizontally(split, rectX, rectWidth);
                    if (attached) {
                        rectWidth -= split.width;
                        if (split.x == rectX) {
                            rectX += split.width;
                        }
                    }
                }
                
                // если нашёлся такой набор окон, снова пробуем найти место для одного окна
                fillWithChildren(windowsLeft, rectX, rectY, rectWidth, rectHeight);
            }
        }
    }
    
    private SplitWindowElement putInSplit(List<WindowElement> windows, int rectX, int rectY, int rectWidth, int rectHeight) {
        SplitWindowElement splitWindow = null;
        List<WindowElement> splitChildren = null;

        // vertical
        for (int i = 0; i < windows.size() - 1; i++) {
            WindowElement first = windows.get(i);
            List<WindowElement> inOneLine = new ArrayList<>(Arrays.asList(first));
            for (int j = i + 1; j < windows.size(); j++) {
                WindowElement second = windows.get(j);
                if (first.x == second.x && first.width == second.width && first.y != second.y) {
                    inOneLine.add(second);
                }
            }
            if (inOneLine.size() > 1) {
                List<WindowElement> winds = findLineFilling(inOneLine, true, rectHeight);
                if (winds != null) {
                    WindowElement firstWindow = winds.get(0);
                    // дополнительно проверим, что ряд окон прижат к одному из краёв
                    if (firstWindow.x == rectX || firstWindow.x + firstWindow.width == rectX + rectWidth) {
                        splitWindow = new SplitWindowElement(main, firstWindow.x, rectY, firstWindow.width, rectHeight);
                        splitChildren = winds;
                    }
                }
            }
        }
        // horizontal
        if (splitWindow == null) {
            for (int i = 0; i < windows.size() - 1; i++) {
                WindowElement first = windows.get(i);
                List<WindowElement> inOneLine = new ArrayList<>(Arrays.asList(first));
                for (int j = i + 1; j < windows.size(); j++) {
                    WindowElement second = windows.get(j);
                    if (first.y == second.y && first.height == second.height && first.x != second.x) {
                        inOneLine.add(second);
                    }
                }
                if (inOneLine.size() > 1) {
                    List<WindowElement> winds = findLineFilling(inOneLine, false, rectWidth);
                    if (winds != null) {
                        WindowElement firstWindow = winds.get(0);
                        if (firstWindow.y == rectY || firstWindow.y + firstWindow.height == rectY + rectHeight) {
                            splitWindow = new SplitWindowElement(main, rectX, firstWindow.y, rectWidth, firstWindow.height);
                            splitChildren = winds;
                        }
                    }
                }
            }
        }

        if (splitChildren != null) {
            for (WindowElement window : splitChildren) {
                splitWindow.addElement(window);
            }
        }
        
        return splitWindow;
    }

    private List<WindowElement> findLineFilling(List<WindowElement> windows, boolean vertical, int dimension) {
        // здесь подразумевается, что все найденные окна одной ширины/высоты, расположены в один ряд без наложений и пробелов
        int sum = 0;
        List<WindowElement> result = new ArrayList<>();

        for (WindowElement window : windows) {
            sum += vertical ? window.height : window.width;
            result.add(window);
        }
        return sum == dimension ? result : null;
    }

    private boolean attachHorizontally(WindowElement window, int rectX, int rectWidth) {
        if (window.x == rectX) {
            splitPanel.addWest(window.initializeView(), window.getPixelWidth());
            addStyleClasses(window);
            windowDirections.put(window, WEST);
            return true;
        } else if (window.x + window.width == rectX + rectWidth) {
            splitPanel.addEast(window.initializeView(), window.getPixelWidth());
            addStyleClasses(window);
            windowDirections.put(window, EAST);
            return true;
        }
        return false;
    }

    private boolean attachVertically(WindowElement window, int rectY, int rectHeight) {
        if (window.y == rectY) {
            splitPanel.addNorth(window.initializeView(), window.getPixelHeight());
            addStyleClasses(window);
            windowDirections.put(window, NORTH);
            return true;
        } else if (window.y + window.height == rectY + rectHeight) {
            splitPanel.addSouth(window.initializeView(), window.getPixelHeight());
            addStyleClasses(window);
            windowDirections.put(window, SOUTH);
            return true;
        }
        return false;
    }

    private void addStyleClasses(WindowElement window) {
        if (window instanceof SimpleWindowElement) {
            window.getView().addStyleName(SIMPLE_WINDOW_CHILD_STYLE);
        }
    }

    public Widget getView() {
        return splitPanel;
    }
    
    public boolean isChildVisible(WindowElement child) {
        return children.get(child);
    }

    @Override
    public void setWindowVisible(WindowElement window) {
        children.put(window, true);
        if (splitPanel.getWidgetIndex(window.getView()) == -1) {
            redraw();
        }
    }

    @Override
    public void setWindowInvisible(WindowElement window) {
        children.put(window, false);
        Widget windowView = window.getView();
        if (splitPanel.getWidgetIndex(windowView) != -1) {
            DockLayoutPanel.Direction direction = windowDirections.get(window);
            Double widgetSize = splitPanel.getWidgetSize(windowView);
            if (direction == NORTH || direction == SOUTH) {
                window.pixelHeight = widgetSize;
            } else if (direction == EAST || direction == WEST) {
                window.pixelWidth = widgetSize;
            }
            redraw();
        }
    }

    public void redraw() {
        splitPanel.clear();
        
        ArrayList<WindowElement> visibleWindows = new ArrayList<>();
        for (WindowElement window : windowDirections.keySet()) {
            if (isChildVisible(window)) {
                visibleWindows.add(window);
            }
        }
        if (!visibleWindows.isEmpty()) {
            for (int i = 0; i < visibleWindows.size() - 1; i++) { // оставляем одно окно, чтобы положить его в центр
                WindowElement window = visibleWindows.get(i);
                
                DockLayoutPanel.Direction direction = windowDirections.get(window);
                Widget windowView = window.getView();
                if (direction == NORTH) {
                    splitPanel.addNorth(windowView, window.getPixelHeight()); // нужно проверить. скорее всего тут нужно не getter'ы использовать. иначе с fullScreenMode будет прыгать
                } else if (direction == SOUTH) {
                    splitPanel.addSouth(windowView, window.getPixelHeight());
                } else if (direction == WEST) {
                    splitPanel.addWest(windowView, window.getPixelWidth());
                } else  {
                    splitPanel.addEast(windowView, window.getPixelWidth());
                }
            }
            splitPanel.add(visibleWindows.get(visibleWindows.size() - 1).getView());
        }
        setVisible(splitPanel.getWidgetCount() > 0);
    }
    
    public void setBorderWindowsHidden(boolean hidden) {
        for (WindowElement windowElement : windowDirections.keySet()) {
            if (isChildVisible(windowElement) && windowDirections.get(windowElement) != CENTER) {
                splitPanel.setWidgetHidden(windowElement.getView(), hidden);
            }
        }
    }
    
    @Override
    protected void setChildSize(WindowElement child) {
        if (isChildVisible(child)) {
            Widget childView = child.getView();
            if (splitPanel.getWidgetDirection(childView) != CENTER) {
                splitPanel.setWidgetSize(childView, getChildSize(child));
            }
        }
    }
    
    private double getChildSize(WindowElement child) { // not CENTER
        DockLayoutPanel.Direction direction = windowDirections.get(child);
        if (direction == NORTH || direction == SOUTH) {
            return child.pixelHeight;
        } else {
            assert direction == EAST || direction == WEST;
            return child.pixelWidth;
        }
    }

    @Override
    public String getSID() {
        return getSID(children.keySet());
    }

    @Override
    public void storeWindowsSizes(Storage storage) {
        for (WindowElement child : children.keySet()) {
            if (windowDirections.get(child) != CENTER) {
                StringBuilder widgetSize = new StringBuilder();
                if (isChildVisible(child)) {
                    widgetSize.append(splitPanel.getWidgetSize(child.getView()));
                } else {
                    widgetSize.append(getChildSize(child));
                }
                storage.setItem(child.getStorageSizeKey(), widgetSize.toString());
            }
            child.storeWindowsSizes(storage);
        }
    }

    @Override
    public void restoreWindowsSizes(Storage storage) {
        for (WindowElement child : children.keySet()) {
            DockLayoutPanel.Direction direction = windowDirections.get(child);
            if (direction != CENTER) {
                String sizeString = storage.getItem(child.getStorageSizeKey());
                Double storedSize = sizeString != null && !sizeString.equals("null") ? Double.valueOf(sizeString) : null; // it seems that somewhy sizeString can be null
                if (storedSize != null) {
                    if (direction == NORTH || direction == SOUTH) {
                        child.pixelHeight = storedSize;
                    } else if (direction == EAST || direction == WEST) {
                        child.pixelWidth = storedSize;
                    }
                    if (isChildVisible(child)) {
                        Widget childView = child.getView();
                        if (splitPanel.getWidgetDirection(childView) != CENTER) {
                            splitPanel.setWidgetSize(childView, storedSize); // для сохранения при setInvisible
                        }
                    }
                    child.sizeStored = true;
                }
            }
            child.restoreWindowsSizes(storage);
        }
    }
    
    public void resetDefaultSizes() {
        for (WindowElement windowElement : children.keySet()) {
            if (children.get(windowElement)) {
                Widget windowView = windowElement.getView();
                DockLayoutPanel.Direction direction = splitPanel.getWidgetDirection(windowView);
                if (direction == NORTH || direction == SOUTH) {
                    splitPanel.setWidgetSize(windowView, windowElement.getInitialHeight());
                } else if (direction != CENTER) {
                    splitPanel.setWidgetSize(windowView, windowElement.getInitialWidth());
                }

                if (windowElement instanceof SplitWindowElement) {
                    ((SplitWindowElement) windowElement).resetDefaultSizes();
                }
            }
        }
    }
}
