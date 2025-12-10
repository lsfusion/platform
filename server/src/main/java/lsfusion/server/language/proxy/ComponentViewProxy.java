package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.language.ElementClassProxy;
import lsfusion.server.language.converters.FontInfoConverter;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.awt.*;

public class ComponentViewProxy<T extends ComponentView> extends ViewProxy<T> implements ElementClassProxy {
    public ComponentViewProxy(T target) {
        super(target);
    }

    @SuppressWarnings("unused")
    public void setSpan(int span) {
        target.setSpan(span, getVersion());
    }

    @SuppressWarnings("unused")
    public void setDefaultComponent(boolean defaultComponent) {
        target.setDefaultComponent(defaultComponent, getVersion());
    }

    @SuppressWarnings("unused")
    public void setActivated(boolean activated) {
        target.setActivated(activated, getVersion());
    }

    /* ========= constraints properties ========= */

    @SuppressWarnings("unused")
    public void setFill(double fill) {
        setFlex(fill);
        setAlignment(fill == 0 ? FlexAlignment.START : FlexAlignment.STRETCH);
    }

    @SuppressWarnings("unused")
    public void setSize(Dimension size) {
        target.setSize(size, getVersion());
    }
    @SuppressWarnings("unused")
    public void setHeight(int prefHeight) {
        target.setHeight(prefHeight, getVersion());
    }
    @SuppressWarnings("unused")
    public void setWidth(int prefWidth) {
        target.setWidth(prefWidth, getVersion());
    }

    @SuppressWarnings("unused")
    public void setFlex(double flex) {
        target.setFlex(flex, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShrink(boolean shrink) {
        target.setShrink(shrink, getVersion());
    }

    @SuppressWarnings("unused")
    public void setAlignShrink(boolean alignShrink) {
        target.setAlignShrink(alignShrink, getVersion());
    }

    @SuppressWarnings("unused")
    public void setAlign(FlexAlignment alignment) {
        setAlignment(alignment);
    }

    @SuppressWarnings("unused")
    public void setAlignment(FlexAlignment alignment) {
        target.setAlignment(alignment, getVersion());
    }

    @SuppressWarnings("unused")
    public void setAlignCaption(boolean alignCaption) {
        target.setAlignCaption(alignCaption, getVersion());
    }

    @SuppressWarnings("unused")
    public void setOverflowHorz(String overflowHorz) {
        target.setOverflowHorz(overflowHorz, getVersion());
    }

    @SuppressWarnings("unused")
    public void setOverflowVert(String overflowVert) {
        target.setOverflowVert(overflowVert, getVersion());
    }

    @SuppressWarnings("unused")
    public void setMarginTop(int marginTop) {
        target.setMarginTop(marginTop, getVersion());
    }

    @SuppressWarnings("unused")
    public void setMarginBottom(int marginBottom) {
        target.setMarginBottom(marginBottom, getVersion());
    }

    @SuppressWarnings("unused")
    public void setMarginLeft(int marginLeft) {
        target.setMarginLeft(marginLeft, getVersion());
    }

    @SuppressWarnings("unused")
    public void setMarginRight(int marginRight) {
        target.setMarginRight(marginRight, getVersion());
    }

    @SuppressWarnings("unused")
    public void setMargin(int margin) {
        target.setMargin(margin, getVersion());
    }

    /* ========= design properties ========= */

    @SuppressWarnings("unused")
    public void setCaptionFont(FontInfo captionFont) {
        target.setCaptionFont(captionFont, getVersion());
    }

    @SuppressWarnings("unused")
    public void setFont(Object font) {
        if (font instanceof PropertyObjectEntity) {
            throw new UnsupportedOperationException("Dynamic font is supported only for propertyDraw");
        } else {
            target.setFont(FontInfoConverter.convertToFontInfo(font.toString()), getVersion());
        }
    }

    @SuppressWarnings("unused")
    public void setClass(Object elementClass) {
        if(elementClass instanceof LocalizedString)
            target.setElementClass(elementClass.toString(), getVersion());
        else
            target.setPropertyElementClass((PropertyObjectEntity<?>) elementClass, getVersion());
    }

    @SuppressWarnings("unused")
    public void setFontSize(int fontSize) {
        FontInfo font = target.getFontNF(getVersion());
        target.setFont(font != null ? font.derive(fontSize) : new FontInfo(fontSize), getVersion());
    }

    @SuppressWarnings("unused")
    public void setFontStyle(LocalizedString lFontStyle) {
        boolean bold;
        boolean italic;
        String fontStyle = lFontStyle.getSourceString();
        //чтобы не заморачиваться с лишним типом для стиля просто перечисляем все варианты...
        if ("bold".equals(fontStyle)) {
            bold = true;
            italic = false;
        } else if ("italic".equals(fontStyle)) {
            bold = false;
            italic = true;
        } else if ("bold italic".equals(fontStyle) || "italic bold".equals(fontStyle)) {
            bold = true;
            italic = true;
        } else if ("".equals(fontStyle)) {
            bold = false;
            italic = false;
        } else {
            throw new IllegalArgumentException("fontStyle value must be a combination of strings bold and italic");
        }

        FontInfo font = target.getFontNF(getVersion());
        target.setFont(font != null ? font.derive(bold, italic) : new FontInfo(bold, italic), getVersion());
    }

    @SuppressWarnings("unused")
    public void setBackground(Object background) {
        if (background instanceof PropertyObjectEntity) {
            throw new UnsupportedOperationException("Dynamic background is supported only for propertyDraw");
        } else {
            target.setBackground((Color) background, getVersion());
        }
    }

    @SuppressWarnings("unused")
    public void setForeground(Object foreground) {
        if (foreground instanceof PropertyObjectEntity) {
            throw new UnsupportedOperationException("Dynamic foreground is supported only for propertyDraw");
        } else {
            target.setForeground((Color) foreground, getVersion());
        }
    }

    // deprecated
    @SuppressWarnings("unused")
    public void setPanelCaptionVertical(boolean panelCaptionVertical) {
        target.setCaptionVertical(panelCaptionVertical, getVersion());
    }

    // deprecated
    @SuppressWarnings("unused")
    public void setPanelCaptionLast(boolean panelCaptionLast) {
        target.setCaptionLast(panelCaptionLast, getVersion());
    }

    // deprecated
    @SuppressWarnings("unused")
    public void setPanelCaptionAlignment(FlexAlignment panelCaptionAlignment) {
        target.setCaptionAlignmentHorz(panelCaptionAlignment, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionVertical(boolean captionVertical) {
        target.setCaptionVertical(captionVertical, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionLast(boolean captionLast) {
        target.setCaptionLast(captionLast, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionAlignmentHorz(FlexAlignment captionAlignment) {
        target.setCaptionAlignmentHorz(captionAlignment, getVersion());
    }

    @SuppressWarnings("unused")
    public void setCaptionAlignmentVert(FlexAlignment captionAlignment) {
        target.setCaptionAlignmentVert(captionAlignment, getVersion());
    }

    @SuppressWarnings("unused")
    public void setShowIf(PropertyObjectEntity<?> showIf) {
        target.setShowIf(showIf, getVersion());
    }
}
