package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import static lsfusion.gwt.client.base.GwtClientUtils.getOffsetSize;

public class PropertyPanelRenderer extends PanelRenderer {

    private final FlexPanel panel;

    private final HTML label;
    
    private final boolean vertical;

    public PropertyPanelRenderer(final GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        super(form, property, columnKey, "dataPanelRendererGridPanel");

        vertical = property.panelCaptionAbove;

        panel = new Panel(vertical);
        panel.addStyleName("dataPanelRendererPanel");

        label = new HTML();
        label.addStyleName("customFontPresenter");
        if (this.property.captionFont != null) {
            this.property.captionFont.apply(label.getElement().getStyle());
        }
        panel.add(label, GFlexAlignment.CENTER);

        value.addFill(panel);

        appendCorners(property);

        finalizeInit();
    }

    public void appendCorners(GPropertyDraw property) {
        if (property.notNull) {
            DivElement changeEventSign = Document.get().createDivElement();
            changeEventSign.addClassName("rightBottomCornerTriangle");
            changeEventSign.addClassName("notNullCornerTriangle");
            value.getRenderElement().appendChild(changeEventSign);
        } else if (property.hasChangeAction) {
            DivElement changeEventSign = Document.get().createDivElement();
            changeEventSign.addClassName("rightBottomCornerTriangle");
            changeEventSign.addClassName("changeActionCornerTriangle");
            value.getRenderElement().appendChild(changeEventSign);
        }
    }

    @Override
    public Widget getComponent() {
        return panel;
    }

    @Override
    protected Widget getTooltipWidget() {
        return label;
    }

    protected void setLabelText(String text) {
        label.setHTML(SafeHtmlUtils.fromString(text));
        if (!vertical) {
            label.getElement().getStyle().setMarginRight(text.isEmpty() ? 0 : 4, Style.Unit.PX);
        }
    }

    private class Panel extends FlexPanel {
        public Panel(boolean vertical) {
            super(vertical);
        }

        @Override
        public Dimension getMaxPreferredSize() {
            Dimension pref = getOffsetSize(label);
            if (!vertical) {
                pref.width += 4; //extra for right label margin
            }

            //+extra for borders and margins
            int width = property.getValueWidthWithPadding(null) + 4;
            int height = property.getValueHeight(null) + 4;

            if (isVertical()) {
                pref.width = Math.max(pref.width, width);
                pref.height += height;
            } else {
                pref.width += width;
                pref.height = Math.max(pref.height, height);
            }

            return pref;
        }
    }
}
