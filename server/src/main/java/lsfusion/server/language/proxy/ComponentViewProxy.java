package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.ComponentDesign;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.awt.*;

public class ComponentViewProxy<T extends ComponentView> extends ViewProxy<T> {
    public ComponentViewProxy(T target) {
        super(target);
    }

    public void setAutoSize(boolean autoSize) {
        target.autoSize = autoSize;
    }

    public void setSpan(int span) {
        target.span = span;
    }

    public void setDefaultComponent(boolean defaultComponent) {
        target.defaultComponent = defaultComponent;
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

    public void setAlign(FlexAlignment alignment) {
        setAlignment(alignment);
    }

    public void setAlignment(FlexAlignment alignment) {
        target.setAlignment(alignment);
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

    public void setFont(FontInfo font) {
        target.design.setFont(font);
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

    public void setBackground(Color background) {
        target.design.background = background;
    }

    public void setForeground(Color foreground) {
        target.design.foreground = foreground;
    }

    public void setImagePath(LocalizedString imagePath) {
        target.design.setImage(imagePath.getSourceString());
    }
}
