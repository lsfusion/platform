package platform.server.form.view.report;

/**
 * User: DAle
 * Date: 11.08.2010
 * Time: 11:17:15
 */

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;


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

    public static String createParamString(String paramName) {
        return "$P{" + paramName + "}";
    }
}
