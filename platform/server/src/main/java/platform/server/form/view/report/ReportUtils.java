package platform.server.form.view.report;

/**
 * User: DAle
 * Date: 11.08.2010
 * Time: 11:17:15
 */

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.StretchTypeEnum;


class ReportUtils {
    public static JRDesignParameter createParameter(String name, Class cls) {
        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setName(name);
        parameter.setValueClass(cls);
        return parameter;
    }

    public static void addParameter(JasperDesign design, String name, Class cls) throws JRException {
        design.addParameter(createParameter(name, cls));
    }

    public static JRDesignExpression createExpression(String text, Class cls) {
        JRDesignExpression subreportExpr = new JRDesignExpression();
        subreportExpr.setValueClass(cls);
        subreportExpr.setText(text);
        return subreportExpr;
    }

    public static JRDesignTextField createTextField(JRDesignStyle style, JRDesignExpression expr) {
        JRDesignTextField field = new JRDesignTextField();
        field.setStyle(style);
        field.setExpression(expr);
        field.setStretchWithOverflow(true);
        field.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
        return field;
    }

    public static JRDesignField createField(String name) {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        return field;
    }

    public static String createParamString(String paramName) {
        return "$P{" + paramName + "}";
    }

    public static String createFieldString(String fieldName) {
        return "$F{" + fieldName + "}"; 
    }
}
