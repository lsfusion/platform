package lsfusion.gwt.form.server.convert;

import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.changes.dto.ColorDTO;
import lsfusion.interop.FontInfo;

import java.awt.*;

public class StaticConverters {

    public static ColorDTO convertColor(Color color) {
        return new ColorDTO(Integer.toHexString(color.getRGB()).substring(2, 8));
    }

    public static GFont convertFont(FontInfo clientFont) {
        if (clientFont == null) {
            return null;
        }
        return new GFont(
                clientFont.getFontFamily(),
                clientFont.getFontSize(),
                clientFont.isBold(),
                clientFont.isItalic()
        );
    }
}
