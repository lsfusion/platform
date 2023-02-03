package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.view.MainFrame;

public class CaptionPanel extends FlexPanel {
    protected Widget header;

    private final boolean border;

    private boolean emptyCaption;

    @Override
    public Border getOuterTopBorder() {
        return border ? Border.HAS : Border.HAS_MARGIN; // captioned not bordered container already "margined" (with min-height, label paddings)
    }

    @Override
    public Border getOuterRestBorder() { // except the top border
        return border ? Border.HAS : Border.NEED;
    }

    private boolean waitingForElement;

    public static class Header extends SimpleWidget {

        public Header(String tagName) {
            super(tagName);
        }

        // header css classes should correspond header classes
        public final static GFlexAlignment HORZ = GFlexAlignment.START;
        public final static GFlexAlignment VERT = GFlexAlignment.CENTER;
    }

    public CaptionPanel(String caption, boolean border) {
        super(true);

        this.border = border;

//        headerButton = new UnFocusableImageButton();
//        headerButton.setEnabled(false);
        header = new Header(MainFrame.useBootstrap ? "h6" : DivElement.TAG);
        header.addStyleName("text-secondary");
//        header.addStyleName("fw-semibold");
        header.addStyleName("fw-normal");

        add(header, GFlexAlignment.STRETCH);

        setCaption(caption);

        if(border) {
            addStyleName("card");
            header.addStyleName("card-header");
//          headerButton.addStyleName("accordion-button");
        } else {
            addStyleName("caption-panel");
            header.addStyleName("caption-panel-header");
        }

        waitingForElement = true;
    }
    public CaptionPanel(String caption, Widget content) {
        this(caption, false);

        addFill(content);
    }

    @Override
    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink, GSize flexBasis) {
        super.add(widget, beforeIndex, alignment, flex, shrink, flexBasis);

        if(waitingForElement) {
            if (border) {
                widget.addStyleName("card-body");
//        widget.addStyleName("accordion-body");
//        widget.addStyleName("accordion-collapse");
            } else {
                widget.addStyleName("caption-panel-body");
            }
        }
    }

    public void setCaption(String caption) {
        GwtClientUtils.setInnerContent(header.getElement(), caption);

        boolean emptyCaption = caption == null || caption.isEmpty();
        if (this.emptyCaption != emptyCaption) {
            if (emptyCaption) {
                header.addStyleName("empty-caption");
//                header.addStyleName("d-none");
                header.setVisible(false); // we need setVisible since isVisible is used, for example in getLines for the resize mechanism
            } else {
                header.removeStyleName("empty-caption");
//                header.removeStyleName("d-none");
                header.setVisible(true);
            }
            this.emptyCaption = emptyCaption;
        }
    }
}
