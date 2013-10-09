package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.shared.view.GComponent;

public abstract class SplitPanelBase<P extends Panel> {
    private final static int splitterSize = 8;

    protected final P panel;

    protected final boolean vertical;
    protected final Splitter splitter;
    protected final DivElement draggerElement;

    protected double flex1;
    protected double flex2;
    protected double currentRatio = -1;
    protected double flexSum;

    protected Widget firstWidget;
    protected Widget secondWidget;

    public SplitPanelBase(boolean vertical, P panel) {
        this.vertical = vertical;

        this.panel = panel;

        splitter = vertical ? new VSplitter() : new HSplitter();

        draggerElement = Document.get().createDivElement();
        draggerElement.addClassName(vertical ? "SplitPanelBase-vdragger" : "SplitPanelBase-hdragger");

        panel.getElement().getStyle().setPosition(Style.Position.RELATIVE);

        addSplitterImpl(splitter);
    }

    public void addFirstWidget(GComponent child, Widget w, double flex) {
        if (firstWidget != null) {
            remove(firstWidget);
        }

        firstWidget = w;

        flex1 = flex <= 0 ? 1 : flex;
        flexSum = flex1 + flex2;

        addFirstWidgetImpl(child, w);
    }

    public void addSecondWidget(GComponent child, Widget w, double flex) {
        if (secondWidget != null) {
            remove(secondWidget);
        }

        secondWidget = w;
        flex2 = flex <= 0 ? 1 : flex;
        flexSum = flex1 + flex2;

        addSecondWidgetImpl(child, w);
    }

    public void remove(Widget child) {
        if (child == firstWidget) {
            panel.remove(firstWidget);
            firstWidget = null;
            flex1 = 0;
            flexSum = flex2;
        } else if (child == secondWidget) {
            panel.remove(secondWidget);
            secondWidget = null;
            flex2 = 0;
            flexSum = flex1;
        }
    }

    protected int getAvailableSize() {
        return (vertical ? panel.getOffsetHeight() : panel.getOffsetWidth()) - (splitter.isVisible() ? splitterSize : 0);
    }

    public int getSplitterSize() {
        return splitterSize;
    }

    public P asWidget() {
        return panel;
    }

    public void update() {
        boolean firstVisible = firstWidget != null && firstWidget.isVisible();
        boolean secondVisible = secondWidget != null && secondWidget.isVisible();

        splitter.setVisible(firstVisible && secondVisible);

        adjustSize();
    }

    private void resize(int firstPixelSize) {
        if (firstWidget == null || secondWidget == null) {
            return;
        }

        currentRatio = firstPixelSize / (double) getAvailableSize();
        adjustSize();

        if (panel instanceof RequiresResize) {
            ((RequiresResize)panel).onResize();
        }
    }

    private void adjustSize() {
        if (firstWidget == null && secondWidget == null) {
            return;
        }
        if (firstWidget == null || !firstWidget.isVisible()) {
            setChildrenRatio(0);
        } else if (secondWidget == null || !secondWidget.isVisible()) {
            setChildrenRatio(1);
        } else {
            if (currentRatio <= 0) {
                currentRatio = flex1 / flexSum;
            }
            setChildrenRatio(currentRatio);
        }
    }

    protected abstract void addSplitterImpl(Splitter splitter);
    protected abstract void addFirstWidgetImpl(GComponent child, Widget widget);
    protected abstract void addSecondWidgetImpl(GComponent child, Widget secondWidget);
    protected abstract void setChildrenRatio(double ratio);

    protected abstract class Splitter extends Widget {
        private boolean dragging;

        public Splitter() {
            setElement(Document.get().createDivElement());
            sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE);
        }


        @Override
        public void onBrowserEvent(Event event) {
            switch (event.getTypeInt()) {
                case Event.ONMOUSEDOWN:
                    Event.setCapture(getElement());
                    event.preventDefault();
                    dragging = installDragger(currentPosition(event));
                    break;
                case Event.ONMOUSEMOVE:
                    if (dragging) {
//                        resize(currentPosition(event));
                        moveDragger(currentPosition(event));
                    }
                    break;
                case Event.ONMOUSEUP:
                    if (dragging) {
                        dragging = false;
                        Event.releaseCapture(getElement());
                        event.preventDefault();
                        uninstallDragger();
                        resize(currentPosition(event));
                    }
                    break;
            }
        }

        private int currentPosition(Event event) {
            int position = getEventPosition(event);
            //оставляем хотя бы по одному пикселу, чтобы значение flex не превратилось в 0
            if (position <= 0) {
                position = 1;
            } else if (position >= getAvailableSize()) {
                position = position - 1;
            }
            return position;
        }

        protected abstract int getEventPosition(Event event);

        private boolean installDragger(int position) {
            int offsetSize = getAvailableSize();
            if (offsetSize <= 2) {
                return false;
            }

            if (vertical) {
                draggerElement.getStyle().setLeft(0, Style.Unit.PX);
                draggerElement.getStyle().setRight(0, Style.Unit.PX);
                draggerElement.getStyle().setHeight(5, Style.Unit.PX);
            } else {
                draggerElement.getStyle().setTop(0, Style.Unit.PX);
                draggerElement.getStyle().setBottom(0, Style.Unit.PX);
                draggerElement.getStyle().setWidth(5, Style.Unit.PX);
            }

            panel.getElement().appendChild(draggerElement);
            moveDragger(position);
            return true;
        }

        private void uninstallDragger() {
            draggerElement.removeFromParent();
        }

        private void moveDragger(int position) {
            if (vertical) {
                draggerElement.getStyle().setTop(position, Style.Unit.PX);
            } else {
                draggerElement.getStyle().setLeft(position, Style.Unit.PX);
            }
        }
    }

    protected class HSplitter extends Splitter {
        public HSplitter() {
            setStyleName("gwt-SplitLayoutPanel-HDragger");
            setWidth(splitterSize + "px");
        }

        @Override
        protected int getEventPosition(Event event) {
            return event.getClientX() - panel.getAbsoluteLeft();
        }
    }

    protected class VSplitter extends Splitter {
        public VSplitter() {
            setStyleName("gwt-SplitLayoutPanel-VDragger");
            setHeight(splitterSize + "px");
        }

        @Override
        protected int getEventPosition(Event event) {
            return event.getClientY() - panel.getAbsoluteTop();
        }
    }
}
