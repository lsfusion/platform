package platform.gwt.form2.client.window;

import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.shared.view.window.GAbstractWindow;

import java.util.*;

public abstract class WindowContainer extends SplitLayoutPanel {
    private Map<GAbstractWindow, TabbedWindowElement> windowsInTabs = new HashMap<GAbstractWindow, TabbedWindowElement>();

    // хрнаним направления, чтобы знать, куда вставлять элемент при его видимости
    private LinkedHashMap<WindowElement, DockLayoutPanel.Direction> windowElements = new LinkedHashMap<WindowElement, Direction>();
    private Map<GAbstractWindow, WindowElement> allWindows = new HashMap<GAbstractWindow, WindowElement>();

    private int rectX = 0,
            rectY = 0,
            rectHeightLeft = 100,
            rectWidthLeft = 100;

    public void initializeWindows(List<GAbstractWindow> allWindows, GAbstractWindow formsWindow) {
        // сперва находим окна с идентичными координатами и кладём их в табы
        initializeTabs(allWindows);
        // затем распределяем остальные окна по WindowElement'ам, складывая их в сплиты при необходимости
        findFillingWindows(allWindows);
        windowElements.put(new SimpleWindowElement(this, formsWindow), Direction.CENTER);
        // заполняем основную панели полученными элементами
        initializeView();
    }

    private void initializeTabs(List<GAbstractWindow> windows) {
        for (int i = 0; i < windows.size() - 1; i++) {
            GAbstractWindow first = windows.get(i);
            TabbedWindowElement frame = null;

            if (!isInTab(first)) {
                for (int j = i + 1; j < windows.size(); j++) {
                    GAbstractWindow second = windows.get(j);
                    if (first.x == second.x && first.y == second.y && first.width == second.width && first.height == second.height) {
                        if (frame == null) {
                            frame = new TabbedWindowElement(this);
                            windowsInTabs.put(first, frame);
                            frame.addWindow(first);
                            WindowElement windowElement = new SimpleWindowElement(this, first);
                            frame.addElement(windowElement);
                            frame.setPosition(first.x, first.y, first.width, first.height);
                            allWindows.put(first, windowElement);
                        }
                        WindowElement windowElement = new SimpleWindowElement(this, second);
                        frame.addWindow(second);
                        frame.addElement(windowElement);
                        windowsInTabs.put(second, frame);
                        allWindows.put(second, windowElement);
                    }
                }
            }
        }
    }

    private boolean isInTab(GAbstractWindow window) {
        return windowsInTabs.containsKey(window);
    }

    private void findFillingWindows(List<GAbstractWindow> windows) {
        // рекурсивно ищем окна, которые заполняют оставшуюся область экрана по ширине или высоте
        if (windows.isEmpty()) {
            return;
        }

        List<GAbstractWindow> windowsLeft = new ArrayList<GAbstractWindow>(windows);
        for (GAbstractWindow window : windows) {
            boolean attached = false;
            if (windowsLeft.contains(window)) {
                if (window.height == rectHeightLeft) {
                    attached = matchHorizontally(window);
                } else if (window.width == rectWidthLeft) {
                    attached = matchVertically(window);
                }
                if (attached) {
                    WindowElement element = isInTab(window) ? windowsInTabs.get(window) : new SimpleWindowElement(this, window);
                    element.setPosition(window.x, window.y, window.width, window.height);
                    if (!isInTab(window)) {
                        allWindows.put(window, element);
                    }
                    windowElements.put(element, null);
                    correctWindowsList(windowsLeft, window);
                }
            }
        }
        if (windowsLeft.size() < windows.size()) {
            findFillingWindows(windowsLeft);
        } else {
            // если ни одно окно  не может полностью покрыть ширину или высоту, пробуем найти набор окон, которые смогут
            // это сделать, и формируем из них SplitPane
            putWindowsInSplits(windowsLeft);
        }
    }

    private void putWindowsInSplits(List<GAbstractWindow> windows) {
        List<GAbstractWindow> windowsLeft = new ArrayList<GAbstractWindow>(windows);
        //vertical
        for (int i = 0; i < windows.size() - 1; i++) {
            GAbstractWindow first = windows.get(i);
            List<GAbstractWindow> inOneLine = new ArrayList<GAbstractWindow>(Arrays.asList(first));
            for (int j = i + 1; j < windows.size(); j++) {
                GAbstractWindow second = windows.get(j);
                if (first.x == second.x && first.width == second.width && first.y != second.y) {
                    inOneLine.add(second);
                }
            }
            if (inOneLine.size() > 1) {
                List<GAbstractWindow> winds = findLineFilling(inOneLine, true);
                if (winds != null) {
                    SplitWindowElement frame = new SplitWindowElement(this, true);
                    boolean attached = matchHorizontally(winds.get(0));
                    if (attached) {
                        for (GAbstractWindow wind : winds) {
                            correctWindowsList(windowsLeft, wind);
                            if (!isInTab(wind)) {
                                WindowElement windowElement = new SimpleWindowElement(this, wind);
                                windowElement.setPosition(wind.x, wind.y, wind.width, wind.height);
                                frame.addElement(windowElement);
                                allWindows.put(wind, windowElement);
                            } else {
                                frame.addElement(windowsInTabs.get(wind));
                            }
                        }
                        frame.setPosition(first.x, rectY, first.width, rectHeightLeft);
                        windowElements.put(frame, null);
                        findFillingWindows(windowsLeft);
                    }
                }
            }
        }
        //horizontal
        for (int i = 0; i < windows.size() - 1; i++) {
            GAbstractWindow first = windows.get(i);
            List<GAbstractWindow> inOneLine = new ArrayList<GAbstractWindow>(Arrays.asList(first));
            for (int j = i + 1; j < windows.size(); j++) {
                GAbstractWindow second = windows.get(j);
                if (first.y == second.y && first.height == second.height && first.x != second.x) {
                    inOneLine.add(second);
                }
            }
            if (inOneLine.size() > 1) {
                List<GAbstractWindow> winds = findLineFilling(inOneLine, false);
                if (winds != null) {
                    SplitWindowElement frame = new SplitWindowElement(this, false);
                    boolean attached = matchVertically(winds.get(0));
                    if (attached) {
                        for (GAbstractWindow wind : winds) {
                            correctWindowsList(windowsLeft, wind);
                            if (!isInTab(wind)) {
                                WindowElement windowElement = new SimpleWindowElement(this, wind);
                                windowElement.setPosition(wind.x, wind.y, wind.width, wind.height);
                                frame.addElement(windowElement);
                                allWindows.put(wind, windowElement);
                            } else {
                                frame.addElement(windowsInTabs.get(wind));
                            }
                        }
                        frame.setPosition(rectX, first.y, rectWidthLeft, first.height);
                        windowElements.put(frame, null);
                        findFillingWindows(windowsLeft);
                    }
                }
            }
        }
    }

    private List<GAbstractWindow> findLineFilling(List<GAbstractWindow> windows, boolean vertical) {
        int sum = 0;
        List<GAbstractWindow> result = new ArrayList<GAbstractWindow>();
        List<GAbstractWindow> frames = new ArrayList<GAbstractWindow>(windows);

        for (GAbstractWindow window : windows) {
            if (frames.contains(window)) {
                if (vertical) {
                    sum += window.height;
                } else {
                    sum += window.width;
                }
                result.add(window);
                if (isInTab(window)) {
                    frames.removeAll(windowsInTabs.get(window).getWindows());
                }
            }
        }
        if (vertical && sum == rectHeightLeft || !vertical && sum == rectWidthLeft) {
            return result;
        }
        return null;
    }

    private void correctWindowsList(List<GAbstractWindow> windows, GAbstractWindow windowToRemove) {
        if (isInTab(windowToRemove))  {
            windows.removeAll(windowsInTabs.get(windowToRemove).getWindows());
        } else {
            windows.remove(windowToRemove);
        }
    }

    private boolean matchHorizontally(GAbstractWindow window) {
        if (window.x == rectX) {
            rectX += window.width;
            rectWidthLeft -= window.width;
            return true;
        } else if (window.x + window.width == rectX + rectWidthLeft) {
            rectWidthLeft -= window.width;
            return true;
        }
        return false;
    }

    private boolean matchVertically(GAbstractWindow window) {
        if (window.y == rectY) {
            rectY += window.height;
            rectHeightLeft -= window.height;
            return true;
        } else if (window.y + window.height == rectY + rectHeightLeft) {
            rectHeightLeft -= window.height;
            return true;
        }
        return false;
    }

    private void initializeView() {
        rectX = 0;
        rectY = 0;
        rectHeightLeft = 100;
        rectWidthLeft = 100;
        drawElements(Arrays.asList(windowElements.keySet().toArray(new WindowElement[windowElements.size()])));
        setDefaultVisible();
    }

    private void drawElements(List<WindowElement> elements) {
        List<WindowElement> windowsLeft = new ArrayList<WindowElement>(elements);
        for (WindowElement window : elements) {
            boolean attached = false;
            if (windowsLeft.contains(window)) {
                if (window.height == rectHeightLeft) {
                    attached = attachHorizontally(window);
                } else if (window.width == rectWidthLeft) {
                    attached = attachVertically(window);
                }
                if (attached) {
                    windowsLeft.remove(window);
                }
            }
        }
        if (windowsLeft.size() > 1) {
            drawElements(windowsLeft);
        } else if (windowsLeft.size() == 1) {
            add(windowsLeft.get(0).getView());
        }
    }

    private boolean attachHorizontally(WindowElement window) {
        if (window.x == rectX) {
            addWest(window.getView(), window.initialWidth);
            rectX += window.width;
            rectWidthLeft -= window.width;
            windowElements.put(window, Direction.WEST);
            return true;
        } else if (window.x + window.width == rectX + rectWidthLeft) {
            addEast(window.getView(), window.initialWidth);
            rectWidthLeft -= window.width;
            windowElements.put(window, Direction.EAST);
            return true;
        }
        return false;
    }

    private boolean attachVertically(WindowElement window) {
        if (window.y == rectY) {
            addNorth(window.getView(), window.initialHeight);
            rectY += window.height;
            rectHeightLeft -= window.height;
            windowElements.put(window, Direction.NORTH);
            return true;
        } else if (window.y + window.height == rectY + rectHeightLeft) {
            addSouth(window.getView(), window.initialHeight);
            rectHeightLeft -= window.height;
            windowElements.put(window, Direction.SOUTH);
            return true;
        }
        return false;
    }

    private void setDefaultVisible() {
        for (GAbstractWindow window : allWindows.keySet()) {
            WindowElement windowElement = allWindows.get(window);
            if (windowElement != null) {
                windowElement.setVisible(window.visible);
            }
        }
    }

    public void updateVisibility(Map<GAbstractWindow, Boolean> windows) {
        for (Map.Entry<GAbstractWindow, Boolean> entry : windows.entrySet()) {
            WindowElement windowElement = allWindows.get(entry.getKey());
            if (windowElement != null) {
                windowElement.setVisible(entry.getValue());
            }
        }
    }

    public void setWindowVisible(WindowElement window) {
        if (getWidgetIndex(window.getView()) == -1) {
            List<WindowElement> mainChildren = new ArrayList<WindowElement>(windowElements.keySet());
            if (mainChildren.indexOf(window) == windowElements.size() - 1) {
                add(window.getView());
            } else {
                for (int i = mainChildren.indexOf(window) + 1; i < mainChildren.size(); i++) {
                    if (getWidgetIndex(mainChildren.get(i).getView()) != -1) {
                        Direction direction = windowElements.get(window);
                        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
                            insert(window.getView(), direction, window.initialHeight, mainChildren.get(i).getView());
                        } else {
                            insert(window.getView(), direction, window.initialWidth, mainChildren.get(i).getView());
                        }
                        return;
                    }
                }
            }
        }
    }

    public void setWindowInvisible(WindowElement window) {
        remove(window.getView());
        forceLayout();
    }

    public void setInitialSize(GAbstractWindow window, int width, int height) {
        WindowElement windowElement = allWindows.get(window);
        if (!windowElement.initialSizeSet && windowElement.getView().isAttached()) {
            if (width != 0 && height != 0) {
                windowElement.changeInitialSize(width, height);
                windowElement.initialSizeSet = true;
            }
        }
    }

    public abstract Widget getWindowView(GAbstractWindow window);
}
