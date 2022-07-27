package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.size.GSize;

public class CaptionPanel extends FlexPanel {
    protected UnFocusableImageButton headerButton;

    private boolean emptyCaption;

    public CaptionPanel(String caption) {
        super(true);
        
        addStyleName("accordion-item");

        headerButton = new UnFocusableImageButton();
        headerButton.setEnabled(false);
        headerButton.addStyleName("accordion-button");

        add(headerButton, GFlexAlignment.STRETCH);

        setCaption(caption);
    }
    public CaptionPanel(String caption, Widget content) {
        this(caption);

        addFillFlex(content, null);
    }

    @Override
    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink, GSize flexBasis) {
        super.add(widget, beforeIndex, alignment, flex, shrink, flexBasis);

        widget.addStyleName("accordion-body");
        widget.addStyleName("accordion-collapse");
    }

    public void setCaption(String caption) {
        GwtClientUtils.setInnerContent(headerButton.getElement(), caption);

        boolean emptyCaption = caption != null && caption.isEmpty();
        if (this.emptyCaption != emptyCaption) {
            if (emptyCaption) {
                headerButton.addStyleName("empty-caption");
            } else {
                headerButton.removeStyleName("empty-caption");
            }
            this.emptyCaption = emptyCaption;
        }
    }
}
