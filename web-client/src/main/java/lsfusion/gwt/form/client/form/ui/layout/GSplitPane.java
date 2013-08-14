package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.base.client.ui.FlexPanel;

public class GSplitPane {
    private final static int splitterSize = 8;

    private final boolean vertical;
    private final FlexPanel panel;
    private final Splitter splitter;

    private double flex1;
    private double flex2;

    private Widget firstWidget;
    private Widget secondWidget;

    public GSplitPane(boolean vertical) {
        this.vertical = vertical;

        panel = new FlexPanel(vertical);
        splitter = vertical ? new VSplitter() : new HSplitter();

        panel.add(splitter, GFlexAlignment.STRETCH);
    }

    public void addFirstWidget(Widget w, double flex) {
        if (firstWidget != null) {
            remove(firstWidget);
        }

        firstWidget = w;
        flex1 = flex <= 0 ? 1 : flex;

        panel.add(firstWidget, 0, GFlexAlignment.STRETCH, flex);

        update();
    }

    public void addSecondWidget(Widget w, double flex) {
        if (secondWidget != null) {
            remove(secondWidget);
        }

        secondWidget = w;
        flex2 = flex <= 0 ? 1 : flex;

        int index = firstWidget == null ? 1 : 2;
        panel.add(secondWidget, index, GFlexAlignment.STRETCH, flex);

        update();
    }

    public void remove(Widget child) {
        if (child == firstWidget) {
            panel.remove(firstWidget);
            firstWidget = null;
        } else if (child == secondWidget) {
            panel.remove(secondWidget);
            secondWidget = null;
        }
        update();
    }

    private int getOffsetSize() {
        return vertical ? panel.getOffsetHeight() : panel.getOffsetWidth();
    }

    public FlexPanel getComponent() {
        return panel;
    }

    private void resize(int firstPixelSize) {
        if (firstWidget == null || secondWidget == null) {
            return;
        }

        int offsetSize = getOffsetSize() - splitterSize;
        if (offsetSize <= 2) {
            return;
        }

        if (firstPixelSize >= offsetSize) {
            //оставляем хотя бы по одному пикселу, чтобы значение flex не превратилось в 0
            firstPixelSize = offsetSize - 1;
        }

        adjustSize(firstPixelSize /(double)offsetSize);
    }

    private void adjustSize(double firstPercentSize) {
        double f1 = (flex1+flex2)*firstPercentSize;
        double f2 = flex1+flex2 - f1;

        panel.setChildFlex(firstWidget, f1);
        panel.setChildFlex(secondWidget, f2);
    }

    public void update() {
        boolean firstVisible = firstWidget != null && firstWidget.isVisible();
        boolean secondVisible = secondWidget != null && secondWidget.isVisible();

        splitter.setVisible(firstVisible && secondVisible);
    }

    class HSplitter extends Splitter {
        public HSplitter() {
            setStyleName("gwt-SplitLayoutPanel-HDragger");
            setWidth(splitterSize + "px");
        }

        @Override
        protected int getEventPosition(Event event) {
            return event.getClientX() - panel.getAbsoluteLeft();
        }
    }

    class VSplitter extends Splitter {
        public VSplitter() {
            setStyleName("gwt-SplitLayoutPanel-VDragger");
            setHeight(splitterSize + "px");
        }

        @Override
        protected int getEventPosition(Event event) {
            return event.getClientY() - panel.getAbsoluteTop();
        }
    }

    abstract class Splitter extends Widget {
        private boolean mouseDown;

        public Splitter() {
            setElement(Document.get().createDivElement());
            sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONMOUSEMOVE);
        }

        @Override
        public void onBrowserEvent(Event event) {
            switch (event.getTypeInt()) {
                case Event.ONMOUSEDOWN:
                    mouseDown = true;
                    Event.setCapture(getElement());
                    event.preventDefault();
                    break;

                case Event.ONMOUSEUP:
                    mouseDown = false;
                    Event.releaseCapture(getElement());
                    event.preventDefault();
                    break;

                case Event.ONMOUSEMOVE:
                    if (mouseDown) {
                        resize(getEventPosition(event));
                    }
                    break;
            }
        }

        protected abstract int getEventPosition(Event event);
    }
}
