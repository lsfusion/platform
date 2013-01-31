package platform.gwt.form.client.form.ui.container;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ui.ResizableHorizontalPanel;
import platform.gwt.base.client.ui.ResizableSimplePanel;
import platform.gwt.base.client.ui.ResizableVerticalPanel;

public class GSplitPane {
    private boolean vertical;
    private CellPanel container;
    private ResizableSimplePanel firstWidget = new ResizableSimplePanel();
    private ResizableSimplePanel secondWidget = new ResizableSimplePanel();
    private Splitter splitter;
    private int splitterSize = 8;

    private String currentFirstSize;

    public GSplitPane(boolean vertical, boolean allowScrolls) {
        this.vertical = vertical;
        container = vertical ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
        splitter = vertical ? new VSplitter() : new HSplitter();

        container.add(firstWidget);
        container.add(splitter);
        container.add(secondWidget);

        container.setSize("100%", "100%");
        firstWidget.setHeight("100%");
        secondWidget.setHeight("100%");


        if (allowScrolls) {
            firstWidget.setStyleName(vertical ? "vSplitPanelContents" : "hSplitPanelContents");
            secondWidget.setStyleName(vertical ? "vSplitPanelContents" : "hSplitPanelContents");
        }

        if (vertical) {
            container.setCellWidth(splitter, "100%");
            container.setCellWidth(firstWidget, "100%");
            container.setCellWidth(secondWidget, "100%");
        } else {
            container.setCellHeight(splitter, "100%");
            container.setCellHeight(firstWidget, "100%");
            container.setCellHeight(secondWidget, "100%");
        }
    }

    public void addWidget(Widget w) {
        if (firstWidget.getWidget() == null) {
            firstWidget.setWidget(w);
        } else {
            secondWidget.setWidget(w);
        }
        update();
    }

    public void remove(Widget child) {
        if (child.equals(firstWidget.getWidget())) {
            firstWidget.remove(child);
        } else if (child.equals(secondWidget.getWidget())) {
            secondWidget.remove(child);
        }
        update();
    }

    public boolean hasChild(Widget child) {
        return child.equals(firstWidget.getWidget()) || child.equals(secondWidget.getWidget());
    }

    private int getOffsetSize() {
        return vertical ? container.getOffsetHeight() : container.getOffsetWidth();
    }

    public CellPanel getComponent() {
        return container;
    }

    private void resize(String firstPercentSize) {
        adjustSize(firstPercentSize);
        currentFirstSize = firstPercentSize;
    }

    private void adjustSize(String firstPercentSize) {
        if (vertical) {
            firstWidget.getElement().getParentElement().getStyle().setProperty("height", firstPercentSize);
            container.setCellHeight(secondWidget, "100%");
        } else {
            firstWidget.getElement().getParentElement().getStyle().setProperty("width", firstPercentSize);
            container.setCellWidth(secondWidget, "100%");
        }
    }

    private void resize(double firstSize) {
        resize((firstSize / getOffsetSize()) * 100 + "%");
    }

    public void setWidgetSize(Widget widget, String width, String height) {
        if (widget.equals(firstWidget.getWidget())) {
            if (vertical && height != null) {
                // на данный момент есть баг GWT + IE:
                // http://code.google.com/p/google-web-toolkit/issues/detail?id=2065
                // поэтому приходится делать так, хотя предполагается использовать container.setCellWidth()
                firstWidget.getElement().getParentElement().getStyle().setProperty("height", height);
            } else if (!vertical && width != null) {
                firstWidget.getElement().getParentElement().getStyle().setProperty("width", width);
            }
        } else if (widget.equals(secondWidget.getWidget())) {
            if (vertical && height != null) {
                secondWidget.getElement().getParentElement().getStyle().setProperty("height", height);
            } else if (!vertical && width != null) {
                secondWidget.getElement().getParentElement().getStyle().setProperty("width", width);
            }
        }
    }

    public void update() {
        firstWidget.setVisible(firstWidget.getWidget() != null && firstWidget.getWidget().isVisible());
        secondWidget.setVisible(secondWidget.getWidget() != null && secondWidget.getWidget().isVisible());

        if (firstWidget.isVisible()) {
            if (secondWidget.isVisible()) {
                if (currentFirstSize != null) {
                    adjustSize(currentFirstSize);
                }
            } else {
                adjustSize("100%");
            }
        } else {
            adjustSize("auto");
        }

        splitter.setVisible(firstWidget.isVisible() && secondWidget.isVisible());
    }

    class HSplitter extends Splitter {
        public HSplitter() {
            setStyleName("gwt-SplitLayoutPanel-HDragger");
            setWidth(splitterSize + "px");
            setHeight("100%");
        }

        @Override
        protected int getEventPosition(Event event) {
            return event.getClientX() - container.getAbsoluteLeft();
        }
    }

    class VSplitter extends Splitter {
        public VSplitter() {
            setStyleName("gwt-SplitLayoutPanel-VDragger");
            setHeight(splitterSize + "px");
            setWidth("100%");
        }

        @Override
        protected int getEventPosition(Event event) {
            return event.getClientY() - container.getAbsoluteTop();
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
                        resize((double) getEventPosition(event));
                    }
                    break;
            }
        }

        protected abstract int getEventPosition(Event event);
    }
}
