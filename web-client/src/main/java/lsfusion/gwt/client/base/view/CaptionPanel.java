package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.ImageHtmlOrTextType;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.view.GFormLayout;

public class CaptionPanel extends FlexPanel {
    public final boolean border;

    private boolean waitingForElement;
    private Widget waitingHeaderLine;

    public CaptionPanel(Widget header, boolean border, boolean vertical, boolean last, GFlexAlignment alignmentHorz, GFlexAlignment alignmentVert) {
        super(vertical);

        this.border = border;

        if(header != null) {
            header.addStyleName("text-secondary");
//        header.addStyleName("fw-semibold");
            header.addStyleName("fw-normal");

            header.addStyleName("caption-panel-header");

//        if(!MainFrame.useBootstrap || border) { // ???
            CaptionPanelHeader headerLine = new CaptionPanelHeader();
            headerLine.setWidget(header);
            headerLine.addStyleName("caption-panel-header-line");
            headerLine.addStyleName(vertical ? "caption-panel-header-line-vert" : "caption-panel-header-line-horz");
            FlexPanelImpl.get().setFlexContentAlignment(headerLine.getElement(), vertical ? alignmentHorz : alignmentVert);

//        }

            if(border)
                headerLine.addStyleName("card-header");

            if(last)
                waitingHeaderLine = headerLine;
            else
                add(headerLine, GFlexAlignment.STRETCH);
        }

        addStyleName("caption-panel");

        if(border) {
            addStyleName("card");
            addStyleName("shadow");
        }

        waitingForElement = true;
    }
    public CaptionPanel(String caption, Widget content) {
        this(caption, null, content);
    }
    public CaptionPanel(String caption, BaseImage image, Widget content) {
        this(createCaptionWidget(caption, image), false, true, false, GFlexAlignment.STRETCH, GFlexAlignment.STRETCH);

        addFill(content);
    }

    // custom caption panels (navigator + system dialogs)
    private static Widget createCaptionWidget(String caption, BaseImage image) {
        Widget captionWidget = GFormLayout.createLabelCaptionWidget();
        BaseImage.initImageText(captionWidget, caption, image, ImageHtmlOrTextType.OTHER);
        return captionWidget;
    }

    @Override
    public void add(Widget widget, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink, GSize flexBasis) {
        super.add(widget, beforeIndex, alignment, flex, shrink, flexBasis);

        if(waitingForElement) {
            widget.addStyleName("caption-panel-body");

            if (border)
                widget.addStyleName("card-body");

            waitingForElement = false;

            if(waitingHeaderLine != null) {
                Widget waitingHeaderLine = this.waitingHeaderLine;
                this.waitingHeaderLine = null;
                add(waitingHeaderLine, GFlexAlignment.STRETCH);
            }
        }
    }
}
