package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.view.MainFrame;

public class CaptionPanel extends FlexPanel {
    public final boolean border;

    private boolean waitingForElement;

    public CaptionPanel(Widget header, boolean border) {
        super(true);

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

            add(headerLine, GFlexAlignment.STRETCH);
//        }

            if(border)
                headerLine.addStyleName("card-header");
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
            widget.addStyleName("caption-panel-body");

            if (border)
                widget.addStyleName("card-body");

            waitingForElement = false;
        }
    }
}
