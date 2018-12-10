package lsfusion.gwt.server.convert;

import lsfusion.gwt.shared.form.view.GFont;
import lsfusion.gwt.shared.form.view.changes.dto.ColorDTO;
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
