package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.view.MainFrame;

public class CaptionPanel extends FlexPanel {
    protected Widget header;

    private final boolean border;

    @Override
    public Border getOuterTopBorder() {
        return border ? Border.HAS : Border.HAS_MARGIN; // captioned not bordered container already "margined" (with min-height, label paddings)
    }

    @Override
    public Border getOuterRestBorder() { // except the top border
        return border ? Border.HAS : Border.NEED;
    }

    private boolean waitingForElement;

    public CaptionPanel(Widget header, boolean border) {
        super(true);

        this.border = border;

        header.addStyleName("text-secondary");
//        header.addStyleName("fw-semibold");
        header.addStyleName("fw-normal");

        add(header, GFlexAlignment.STRETCH);

        if(border) {
            addStyleName("card");
            header.addStyleName("card-header");
//          headerButton.addStyleName("accordion-button");
        } else {
            addStyleName("caption-panel");
            header.addStyleName("caption-panel-header");
        }

        this.header = header;

        waitingForElement = true;
    }
    public CaptionPanel(String caption, Widget content) {
        this(caption, null, content);
    }
    public CaptionPanel(String caption, BaseImage image, Widget content) {
        this(createCaptionWidget(caption, image), false);

        addFill(content);
    }

    // custom caption panels (navigator + system dialogs)
    private static Widget createCaptionWidget(String caption, BaseImage image) {
        Widget captionWidget = GFormLayout.createLabelCaptionWidget();
        BaseImage.initImageText(captionWidget, caption, image, false);
        return captionWidget;
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
}
