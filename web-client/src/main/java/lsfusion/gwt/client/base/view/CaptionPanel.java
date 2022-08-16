package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.view.MainFrame;

public class CaptionPanel extends FlexPanel {
    protected Widget header;

    private boolean border;

    private boolean emptyCaption;

    public CaptionPanel(GContainer container) {
        this(container.caption, container.border);
    }

    @Override
    public FlexPanel.Border getOuterBorder() {
        return border ? Border.HAS : Border.NEED;
    }

    private boolean waitingForElement;

    private CaptionPanel(String caption, boolean border) {
        super(true);

        if(!MainFrame.useBootstrap)
            border = false;

        this.border = border;

//        headerButton = new UnFocusableImageButton();
//        headerButton.setEnabled(false);
        if(MainFrame.useBootstrap)
            header = new SimpleWidget("h6");
        else
            header = new DivWidget();
        header.addStyleName("text-muted");
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
                header.addStyleName("d-none");
            } else {
                header.removeStyleName("empty-caption");
                header.removeStyleName("d-none");
            }
            this.emptyCaption = emptyCaption;
        }
    }
}
