package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FileBasedGridCellRenderer extends AbstractGridCellRenderer {
    protected GPropertyDraw property;

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        element.getStyle().setWhiteSpace(Style.WhiteSpace.PRE);

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) element.setAttribute("align", textAlignStyle.getCssName());

        if (!isSingle) element.getStyle().setPosition(Style.Position.RELATIVE);
    }

    protected void setBasedEmptyElement(Element element){
        element.getStyle().setPaddingRight(4, Style.Unit.PX);
        element.getStyle().setPaddingLeft(4, Style.Unit.PX);
        element.setInnerText(REQUIRED_VALUE);
        element.setTitle(REQUIRED_VALUE);
        element.addClassName("requiredValueString");
    }

    protected FileBasedGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    protected abstract void setFileSrc(ImageElement file, Object value);
}
