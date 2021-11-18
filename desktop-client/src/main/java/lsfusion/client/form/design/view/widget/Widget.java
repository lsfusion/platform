package lsfusion.client.form.design.view.widget;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.design.view.LayoutData;
import lsfusion.interop.base.view.FlexComponent;
import lsfusion.interop.base.view.FlexConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public interface Widget extends FlexComponent {

    JComponent getComponent();

    default boolean isVisible() {
        return getComponent().isVisible();
    }
    default void setVisible(boolean isVisible) {
        getComponent().setVisible(isVisible);
    }
    default Component getParent() {
        return getComponent().getParent();
    }

    default Dimension getPreferredSize() {
        return getComponent().getPreferredSize();
    }

    // actually we can get either width or height from layoutData, but for now it's not that important
    default Integer getFlexWidth() {
        return (Integer) getComponent().getClientProperty("flex.width");
    }
    default void setFlexWidth(Integer width) {
        getComponent().putClientProperty("flex.width", width);
    }
    default Integer getFlexHeight() {
        return (Integer) getComponent().getClientProperty("flex.height");
    }
    default void setFlexHeight(Integer height) {
        getComponent().putClientProperty("flex.height", height);
    }
    default Dimension getFlexPreferredSize(Boolean vertical) {
        Dimension actualSize = getPreferredSize();
        Integer flexWidth = getFlexWidth();
        Integer flexHeight = getFlexHeight();
        return new Dimension(flexWidth != null ? (vertical == null || !vertical ? flexWidth : BaseUtils.max(flexWidth, actualSize.width)) : actualSize.width,
                flexHeight != null ? (vertical == null || vertical ? flexHeight : BaseUtils.max(flexHeight, actualSize.height)) : actualSize.height);
    }
    @Override
    default FlexConstraints getFlexConstraints() {
        LayoutData layoutData = getLayoutData();
        return new FlexConstraints(layoutData.alignment, layoutData.flex, layoutData.shrink);
    }

    default Dimension getMaxPreferredSize() {
        return getPreferredSize();
    }

    static String toString(Widget w, String defaultToString) {
        Object debugContainer = w.getDebugContainer();
        return (debugContainer != null ? debugContainer + " " : "") + "size (" + w.getFlexWidth() + "," + w.getFlexHeight() + ") " + defaultToString;
    }

    default Object getDebugContainer() {
        return getComponent().getClientProperty("debug.container");
    }
    default void setDebugContainer(Object debugContainer) {
        getComponent().putClientProperty("debug.container", debugContainer);
    }

    default LayoutData getLayoutData() {
        return (LayoutData) getComponent().getClientProperty("layout.data");
    }
    default void setLayoutData(LayoutData layoutData) {
        getComponent().putClientProperty("layout.data", layoutData);
    }

    // mouse events
    static void addMouseListeners(Widget w) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                checkAndPropagateMouseEvent(w, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                checkAndPropagateMouseEvent(w, e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                checkAndPropagateMouseEvent(w, e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                checkAndPropagateMouseEvent(w, e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                checkAndPropagateMouseEvent(w, e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                checkAndPropagateMouseEvent(w, e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                super.mouseWheelMoved(e);
                checkAndPropagateMouseEvent(w, e);
            }
        };
        w.getComponent().addMouseListener(mouseAdapter);
        w.getComponent().addMouseMotionListener(mouseAdapter);
        w.getComponent().addMouseWheelListener(mouseAdapter);
    }

    static void checkAndPropagateMouseEvent(Widget w, MouseEvent e) {
        checkAndPropagateMouseEvent(w.getComponent(), e);
    }

    default void checkMouseEvent(MouseEvent e) {
    }

    // unlike web-client swing doesn't propagate mouse events to upper components, so we'll emulate this
    static void checkAndPropagateMouseEvent(Component w, MouseEvent e) {
        if(w instanceof Widget)
            ((Widget)w).checkMouseEvent(e);

        Component parent = w.getParent();
        if(parent != null)
            checkAndPropagateMouseEvent(parent, e);
    }
}
