package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ComponentDesign;
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

    public void setSpan(int span) {
        target.span = span;
    }

    public void setDefaultComponent(boolean defaultComponent) {
        target.defaultComponent = defaultComponent;
    }
    
    public void setActivated(boolean activated) {
        target.activated = activated;
    }

    /* ========= constraints properties ========= */

    public void setFill(double fill) {
        setFlex(fill);
        setAlignment(fill == 0 ? FlexAlignment.START : FlexAlignment.STRETCH);
    }

    public void setSize(Dimension size) {
        target.setSize(size);
    }
    public void setHeight(int prefHeight) {
        target.setHeight(prefHeight);
    }
    public void setWidth(int prefWidth) {
        target.setWidth(prefWidth);
    }

    public void setFlex(double flex) {
        target.setFlex(flex);
    }

    public void setShrink(boolean shrink) {
        target.setShrink(shrink);
    }

    public void setAlignShrink(boolean alignShrink) {
        target.setAlignShrink(alignShrink);
    }

    public void setAlign(FlexAlignment alignment) {
        setAlignment(alignment);
    }

    public void setAlignment(FlexAlignment alignment) {
        target.setAlignment(alignment);
    }

    public void setAlignCaption(boolean alignCaption) {
        target.alignCaption = alignCaption;
    }

    public void setOverflowHorz(String overflowHorz) {
        target.setOverflowHorz(overflowHorz);
    }

    public void setOverflowVert(String overflowVert) {
        target.setOverflowVert(overflowVert);
    }

    public void setMarginTop(int marginTop) {
        target.setMarginTop(marginTop);
    }

    public void setMarginBottom(int marginBottom) {
        target.setMarginBottom(marginBottom);
    }

    public void setMarginLeft(int marginLeft) {
        target.setMarginLeft(marginLeft);
    }

    public void setMarginRight(int marginRight) {
        target.setMarginRight(marginRight);
    }

    public void setMargin(int margin) {
        target.setMargin(margin);
    }

    /* ========= design properties ========= */

    public void setCaptionFont(FontInfo captionFont) {
        target.design.setCaptionFont(captionFont);
    }

    public void setFont(Object font) {
        if (font instanceof PropertyObjectEntity) {
            throw new UnsupportedOperationException("Dynamic font is supported only for propertyDraw");
        } else {
            target.design.setFont(FontInfoConverter.convertToFontInfo(font.toString()));
        }
    }

    public void setClass(Object elementClass) {
        if(elementClass instanceof LocalizedString)
            target.setElementClass(elementClass.toString());
        else
            target.setPropertyElementClass((PropertyObjectEntity<?>) elementClass);
    }

    public void setFontSize(int fontSize) {
        ComponentDesign design = target.design;

        FontInfo font = design.font != null ? design.font.derive(fontSize) : new FontInfo(fontSize);

        design.setFont(font);
    }

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

        ComponentDesign design = target.design;

        FontInfo font = design.font != null ? design.font.derive(bold, italic) : new FontInfo(bold, italic);

        design.setFont(font);
    }

    public void setBackground(Object background) {
        if (background instanceof PropertyObjectEntity) {
            throw new UnsupportedOperationException("Dynamic background is supported only for propertyDraw");
        } else {
            target.design.background = (Color) background;
        }
    }

    public void setForeground(Object foreground) {
        if (foreground instanceof PropertyObjectEntity) {
            throw new UnsupportedOperationException("Dynamic foreground is supported only for propertyDraw");
        } else {
            target.design.foreground = (Color) foreground;
        }
    }

    // deprecated
    public void setPanelCaptionVertical(boolean panelCaptionVertical) {
        target.captionVertical = panelCaptionVertical;
    }

    // deprecated
    public void setPanelCaptionLast(boolean panelCaptionLast) {
        target.captionLast = panelCaptionLast;
    }

    // deprecated
    public void setPanelCaptionAlignment(FlexAlignment panelCaptionAlignment) {
        target.captionAlignmentHorz = panelCaptionAlignment;
    }

    public void setCaptionVertical(boolean captionVertical) {
        target.captionVertical = captionVertical;
    }

    public void setCaptionLast(boolean captionLast) {
        target.captionLast = captionLast;
    }

    public void setCaptionAlignmentHorz(FlexAlignment captionAlignment) {
        target.captionAlignmentHorz = captionAlignment;
    }

    public void setCaptionAlignmentVert(FlexAlignment captionAlignment) {
        target.captionAlignmentVert = captionAlignment;
    }


    public void setShowIf(PropertyObjectEntity<?> showIf) {
        target.setShowIf(showIf);
    }
}
