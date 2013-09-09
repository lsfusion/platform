package lsfusion.interop;

import java.awt.*;
import java.io.Serializable;

public final class FontInfo implements Serializable {
    public final String fontFamily;
    public final int fontSize;
    public final boolean bold;
    public final boolean italic;

    private transient Font font;

    public FontInfo(String fontFamily) {
        this(fontFamily, 0, false, false);
    }

    public FontInfo(int fontSize) {
        this(null, fontSize, false, false);
    }

    public FontInfo(boolean bold, boolean italic) {
        this(null, 0, bold, italic);
    }

    public FontInfo(String fontFamily, int fontSize, boolean bold, boolean italic) {
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.bold = bold;
        this.italic = italic;
    }

    @SuppressWarnings("MagicConstant")
    public Font deriveFrom(Component component) {
        assert component != null;
        if (fontFamily != null && fontSize > 0) {
            if (font == null) {
                font = new Font(fontFamily, getAWTFontStyle(), fontSize);
            }
            return font;
        }

        Font compFont = component.getFont();
        if (fontFamily != null) {
            return new Font(fontFamily, getAWTFontStyle(), compFont.getSize());
        }

        if (compFont.getSize() == fontSize && compFont.isBold() == bold && compFont.isItalic() == italic) {
            return compFont;
        }

        if (fontSize > 0) {
            return compFont.deriveFont(getAWTFontStyle(), fontSize);
        }

        return compFont.deriveFont(getAWTFontStyle());
    }

    public int getAWTFontStyle() {
        int style = 0;
        if (bold) {
            style |= Font.BOLD;
        }
        if (italic) {
            style |= Font.ITALIC;
        }
        return style;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public FontInfo derive(boolean bold, boolean italic) {
        return new FontInfo(fontFamily, fontSize, bold, italic);
    }

    public FontInfo derive(int fontSize) {
        return new FontInfo(fontFamily, fontSize, bold, italic);
    }
}
