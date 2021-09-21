package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;

public class CaptionPanel extends FlexPanel {
    private final String NOT_NULL_CAPTION_CSS = "captionPanelWithLegend";
    
    protected final Label legend;
    private int marginTop = 1;

    public CaptionPanel(String caption, boolean vertical) {
        super(vertical);

        legend = new Label(EscapeUtils.unicodeEscape(caption != null ? caption : ""));
        legend.setStyleName("captionPanelLegend");
        add(legend);

        setStyleName("captionPanel");
        
        if (caption != null) {
            addStyleName(NOT_NULL_CAPTION_CSS);
        }
        
        setMargins(caption);
    }
    public CaptionPanel(String caption, Widget content) {
        this(caption, true);

        addFillFlex(content, null);
    }
    
    private void setMargins(String caption) {
        int paddingTop;
        if ("".equals(caption)) {
            marginTop = 1;
            paddingTop = 1;
        } else if (caption == null) {
            marginTop = 0;
            paddingTop = 0;
        } else {
            marginTop = 9;
            paddingTop = 3;
        }
        Style style = getElement().getStyle();
        style.setMarginTop(marginTop, Style.Unit.PX);
        style.setPaddingTop(paddingTop, Style.Unit.PX);
    } 

    @Override
    public Dimension getMaxPreferredSize() {
        return adjustMaxPreferredSize(GwtClientUtils.calculateMaxPreferredSize(getWidget(1))); // assuming that there are only 2 widgets, and the second is the main widget
    }

    public Dimension adjustMaxPreferredSize(Dimension dimension) {
        return GwtClientUtils.enlargeDimension(dimension,
                2,
                marginTop + (isNotNullCaption() ? 1 + 2 : 0)); // 1 for bottom margin, 2 for border
    }
    
    private boolean isNotNullCaption() {
        return getStyleName().contains(NOT_NULL_CAPTION_CSS);
    }

    public void setCaption(String title) {
        legend.setText(EscapeUtils.unicodeEscape(title != null ? title : ""));
        
        if (title != null) {
            if (!isNotNullCaption()) {
                addStyleName(NOT_NULL_CAPTION_CSS);
            }
        } else {
            removeStyleName(NOT_NULL_CAPTION_CSS);
        }
        
        setMargins(title);
    }
}
