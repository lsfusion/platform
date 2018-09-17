package lsfusion.server.form.view.report;

import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.type.LineStyleEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;

import java.awt.*;

public class DesignStyles {

    public static JRDesignStyle getDefaultStyle() {
        JRDesignStyle style = new JRDesignStyle();
        style.setName("DefaultStyle");

        style.setFontName("lsf.TimesNewRoman");
        style.setFontSize(10);
        style.setVerticalAlignment(VerticalAlignEnum.MIDDLE);

        // используем getParagraph вместо getLineBox, поскольку JasperReports padding у LineBox вообще никак не уважает
        style.getParagraph().setLeftIndent(2);
        style.getParagraph().setRightIndent(2);

//        style.getLineBox().setLeftPadding(2);
//        style.getLineBox().setRightPadding(2);

        style.getLineBox().getPen().setLineColor(Color.black);
        style.getLineBox().getPen().setLineStyle(LineStyleEnum.SOLID);
        style.getLineBox().getPen().setLineWidth((float) 0.5);
        style.setDefault(true);
        return style;
    }

    public static JRDesignStyle getGroupStyle(int groupLevel, int maxTreeLevel) {
        JRDesignStyle style = new JRDesignStyle();
        style.setName("DefaultStyle");

        style.setFontName("lsf.TimesNewRoman");
        style.setFontSize(10);
        style.setVerticalAlignment(VerticalAlignEnum.MIDDLE);

        // используем getParagraph вместо getLineBox, поскольку JasperReports padding у LineBox вообще никак не уважает
        style.getParagraph().setLeftIndent(2);
        style.getParagraph().setRightIndent(2);

//        style.getLineBox().setLeftPadding(2);
//        style.getLineBox().setRightPadding(2);

        style.getLineBox().getPen().setLineColor(Color.black);
        style.getLineBox().getPen().setLineStyle(LineStyleEnum.SOLID);
        style.getLineBox().getPen().setLineWidth((float) 0.5);
        style.setName("GroupCellStyle" + groupLevel);
        if(groupLevel > 0 && groupLevel < maxTreeLevel) { // first and last groups are white (transparent)
            style.setMode(ModeEnum.OPAQUE);
            int color = (int) (255 - 64 * groupLevel / maxTreeLevel);
            style.setBackcolor(new Color(color, color, color));
        }
        return style;
    }
}
