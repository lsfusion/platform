package lsfusion.gwt.cellview.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class HeaderPanel extends Panel implements RequiresResize {
    public final static int DEFAULT_HEADER_HEIGHT = 36;
    public final static int DEFAULT_FOOTER_HEIGHT = 0;

    private Widget content;
    private final Element contentContainer;

    private Widget footer;
    private final Element footerContainer;
    private int fixedHeaderHeight = -1;

    private Widget header;
    private final Element headerContainer;
    private int fixedFooterHeight = -1;

    private int lastContentTop;
    private int lastContentBottom;

    private boolean layoutScheduled = false;

    public HeaderPanel() {
        this(DEFAULT_HEADER_HEIGHT, DEFAULT_FOOTER_HEIGHT);
    }

    public HeaderPanel(int initialHeaderHeight, int initialFooterHeight) {
        lastContentTop = initialHeaderHeight;
        lastContentBottom = initialFooterHeight;

        // Create the outer element
        Element elem = Document.get().createDivElement().cast();
        elem.getStyle().setPosition(Style.Position.RELATIVE);
        elem.getStyle().setOverflow(Style.Overflow.HIDDEN);
        setElement(elem);

        // Create the header container.
        headerContainer = createContainer();
        headerContainer.getStyle().setTop(0, Style.Unit.PX);
        elem.appendChild(headerContainer);

        // Create the footer container.
        footerContainer = createContainer();
        footerContainer.getStyle().setBottom(0.0, Style.Unit.PX);
        elem.appendChild(footerContainer);

        // Create the content container.
        contentContainer = createContainer();
        contentContainer.getStyle().setOverflow(Style.Overflow.HIDDEN);
        contentContainer.getStyle().setTop(lastContentTop, Style.Unit.PX);
        contentContainer.getStyle().setBottom(lastContentBottom, Style.Unit.PX);
        elem.appendChild(contentContainer);
    }

    public void setFixedHeaderHeight(int fixedHeaderHeight) {
        this.fixedHeaderHeight = fixedHeaderHeight;
        scheduledLayout();
    }

    public void setFixedFooterHeight(int fixedFooterHeight) {
        this.fixedFooterHeight = fixedFooterHeight;
        scheduledLayout();
    }

    @Override
    public void add(Widget w) {
        // Add widgets in the order that they appear.
        if (header == null) {
            setHeaderWidget(w);
        } else if (content == null) {
            setContentWidget(w);
        } else if (footer == null) {
            setFooterWidget(w);
        } else {
            throw new IllegalStateException("HeaderPanel already contains header, content, and footer widgets.");
        }
    }

    public Widget getContentWidget() {
        return content;
    }

    public Widget getFooterWidget() {
        return footer;
    }

    public Widget getHeaderWidget() {
        return header;
    }

    public Iterator<Widget> iterator() {
        return new WidgetIterator();
    }

    public void onResize() {
        // Handle the outer element resizing.
//        scheduledLayout();
        forceLayout();
    }

    @Override
    public boolean remove(Widget w) {
        // Validate.
        if (w.getParent() != this) {
            return false;
        }
        // Orphan.
        try {
            orphan(w);
        } finally {
            // Physical detach.
            w.getElement().removeFromParent();

            // Logical detach.
            if (w == content) {
                content = null;
                contentContainer.getStyle().setDisplay(Style.Display.NONE);
            } else if (w == header) {
                header = null;
                headerContainer.getStyle().setDisplay(Style.Display.NONE);
            } else if (w == footer) {
                footer = null;
                footerContainer.getStyle().setDisplay(Style.Display.NONE);
            }
        }
        return true;
    }

    public void setContentWidget(Widget w) {
        contentContainer.getStyle().clearDisplay();
        add(w, content, contentContainer);

        // Logical attach.
        content = w;
        scheduledLayout();
    }

    public void setFooterWidget(Widget w) {
        footerContainer.getStyle().clearDisplay();
        add(w, footer, footerContainer);

        // Logical attach.
        footer = w;
        scheduledLayout();
    }

    public void setHeaderWidget(Widget w) {
        headerContainer.getStyle().clearDisplay();
        add(w, header, headerContainer);

        // Logical attach.
        header = w;
        scheduledLayout();
    }

    private void add(Widget w, Widget toReplace, Element container) {
        // Validate.
        if (w == toReplace) {
            return;
        }

        // Detach new child.
        if (w != null) {
            w.removeFromParent();
        }

        // Remove old child.
        if (toReplace != null) {
            remove(toReplace);
        }

        if (w != null) {
            // Physical attach.
            container.appendChild(w.getElement());

            adopt(w);
        }
    }

    private Element createContainer() {
        Element container = Document.get().createDivElement().cast();
        container.getStyle().setPosition(Style.Position.ABSOLUTE);
        container.getStyle().setDisplay(Style.Display.NONE);
        container.getStyle().setLeft(0.0, Style.Unit.PX);
        container.getStyle().setRight(0.0, Style.Unit.PX);
        return container;
    }

    /**
     * Schedule layout to adjust the height of the content area.
     */
    private void scheduledLayout() {
        if (!layoutScheduled) {
            layoutScheduled = true;
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                public void execute() {
                    layoutScheduled = false;
                    // No sense in doing layout if we aren't attached or have no content.
                    forceLayout();
                }
            });
        }
    }

    private void forceLayout() {
        if (!isAttached() || content == null) {
            return;
        }

        int contentTop = 0;
        int contentBottom = 0;
        if (header != null) {
            contentTop = fixedHeaderHeight >= 0 ? fixedHeaderHeight : headerContainer.getOffsetHeight();
        }
        if (footer != null) {
            contentBottom = fixedFooterHeight >= 0 ? fixedFooterHeight : footerContainer.getOffsetHeight();
        }

        if (contentTop != lastContentTop) {
            contentContainer.getStyle().setTop(contentTop, Style.Unit.PX);
            lastContentTop = contentTop;
        }
        if (contentBottom != lastContentBottom) {
            contentContainer.getStyle().setBottom(contentBottom, Style.Unit.PX);
            lastContentBottom = contentBottom;
        }

        // Provide resize to child.
        if (content instanceof RequiresResize) {
            ((RequiresResize) content).onResize();
        }
    }

    private class WidgetIterator implements Iterator<Widget> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            for (int i = index; i < 3; ++i) {
                if (getMaybeWidget(i) != null) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Widget next() {
            for (int i = index; i < 3; ++i) {
                index = i + 1;

                Widget maybeWidget = getMaybeWidget(i);
                if (maybeWidget != null) {
                    return maybeWidget;
                }
            }
            throw new NoSuchElementException();
        }

        private Widget getMaybeWidget(int index) {
            switch (index) {
                case 0:
                    return header;
                case 1:
                    return content;
                case 2:
                    return footer;
            }
            throw new ArrayIndexOutOfBoundsException(index);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove isn't supported");
        }
    }
}
