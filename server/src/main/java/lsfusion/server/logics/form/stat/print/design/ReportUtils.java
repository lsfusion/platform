package lsfusion.server.logics.form.stat.print.design;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.StretchTypeEnum;

import java.math.BigDecimal;

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
        JRDesignExpression expr = new JRDesignExpression();
        if(cls != null) {
            expr.setValueClass(cls);
        }
        expr.setText(text);
        return expr;
    }

    public static JRDesignPropertyExpression createPropertyExpression(String propertyName, String text, Class cls) {
        JRDesignExpression expr = createExpression(text, cls);
        JRDesignPropertyExpression propertyExpr = new JRDesignPropertyExpression();
        propertyExpr.setValueExpression(expr);
        propertyExpr.setName(propertyName);
        return propertyExpr;
    }
    
    public static JRDesignTextField createTextField(JRDesignStyle style, JRDesignExpression expr, boolean toStretch) {
        JRDesignTextField field = new JRDesignTextField();
        field.setStyle(style);
        field.setExpression(expr);
        field.setStretchWithOverflow(toStretch);
        if (toStretch) {
            field.setStretchType(StretchTypeEnum.RELATIVE_TO_BAND_HEIGHT);
        }
        return field;
    }

    public static JRDesignImage createImageField(JRDesignStyle style, JRDesignExpression expr) {
        JRDesignImage field = new JRDesignImage(null);
        field.setStyle(style);
        field.setExpression(expr);
        return field;
    }

    public static JRDesignField createField(String name, String className) {
        JRDesignField field = new JRDesignField();
        field.setName(name);
        field.setValueClassName(className);
        return field;
    }

    public static String createParamString(String paramName) {
        return "$P{" + paramName + "}";
    }

    public static String createFieldString(String fieldName) {
        return "$F{" + fieldName + "}"; 
    }
    
    // In MS Excel '#,##0.##' pattern shows decimal separator even if number is integer (example: "45," in russian locale) 
    public static String createPatternExpressionForExcelSeparatorProblem(String pattern, String fieldName, Class cls) {
        int dotPosition = pattern.lastIndexOf('.');
        String intPattern = pattern.substring(0, dotPosition);
        if (cls == BigDecimal.class) {
            return String.format("$F{%s}.compareTo($F{%s}.setScale(0, BigDecimal.ROUND_HALF_UP) ) == 0 ? \"%s\" : \"%s\"", fieldName, fieldName, intPattern, pattern);
        } else if (cls == Double.class) {
            return String.format("$F{%s} == Math.floor($F{%s}) ? \"%s\" : \"%s\"", fieldName, fieldName, intPattern, pattern);
        }
        return null;
    }
    
    public static final String EXCEL_SEPARATOR_PROBLEM_REGEX = ".*\\.#+";

    public static String escapeLineBreak(String value) {
        return value.replace("\n", "\\n");
    }
}
