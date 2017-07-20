package lsfusion.server.form.view.report;

import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.type.LineStyleEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;

import java.awt.*;

/**
 * User: DAle
 * Date: 12.08.2010
 * Time: 14:57:23
  */

public class DesignStyles {

    private static void setDefaultStyle(JRDesignStyle style) {
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
    }

    private static void setGroupStyle(JRDesignStyle style, int groupIndex, int groupCount, int minLevel, int maxLevel, int maxTreeLevel) {
        setDefaultStyle(style);
        style.setName("GroupCellStyle" + groupIndex);
        int colorIndex = groupCount - groupIndex - 1;
        double normalizedIntervalLength = (maxLevel - minLevel) / (double) maxTreeLevel;
        int color = (int) (255 - 64 * (minLevel + colorIndex * normalizedIntervalLength) / maxTreeLevel);
        style.setMode(ModeEnum.OPAQUE);
        style.setBackcolor(new Color(color, color, color));
    }

    public static JRDesignStyle getDefaultStyle() {
        JRDesignStyle style = new JRDesignStyle();
        setDefaultStyle(style);
        style.setDefault(true);
        return style;
    }

    public static JRDesignStyle getGroupStyle(int groupIndex, int groupCount, int minLevel, int maxLevel, int maxTreeLevel) {
        JRDesignStyle style = new JRDesignStyle();
        setGroupStyle(style, groupIndex, groupCount, minLevel, maxLevel, maxTreeLevel);
        return style;
    }
}
