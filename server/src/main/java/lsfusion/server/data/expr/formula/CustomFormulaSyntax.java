package lsfusion.server.data.expr.formula;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.sql.syntax.SQLSyntax;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomFormulaSyntax {
    
    private final String defaultSyntax;    
    private final ImMap<SQLSyntaxType, String> customSyntaxes;

    public final ImSet<String> params;

    private Pattern paramsPattern;

    public String getDefaultSyntax() {
        if(defaultSyntax.isEmpty())
            return customSyntaxes.getValue(0);
        return defaultSyntax;
    }

    public CustomFormulaSyntax(String defaultSyntax, ImSet<String> params) {
        this(defaultSyntax, MapFact.EMPTY(), params);
    }

    public CustomFormulaSyntax(String defaultSyntax, ImMap<SQLSyntaxType, String> customSyntaxes, ImSet<String> params) {
        this.defaultSyntax = defaultSyntax;
        this.customSyntaxes = customSyntaxes;

        this.params = params;
        if(!params.isEmpty()) {
            // word boundaries added to be able to match prm10+
            // now we don't need it since we use getParamName or fixed non-postfix params (however maybe it makes sense to add assertion), but just in case
            this.paramsPattern = Pattern.compile("\\b(" + params.toString( "|") + ")\\b");
        } else
            this.paramsPattern = null;
    }

    public String getFormula(SQLSyntaxType type) {
        String formula = customSyntaxes.get(type);
        if(formula != null)
            return formula;
            
        return defaultSyntax;            
    }

    public String getSource(SQLSyntax syntax, ImMap<String, String> paramSources) {
        String formula = getFormula(syntax.getSyntaxType());
        if(paramsPattern != null) {
            Matcher m = paramsPattern.matcher(formula);
            StringBuffer result = new StringBuffer();
            while (m.find()) {
                String param = m.group();
                m.appendReplacement(result, Matcher.quoteReplacement(paramSources.get(param)));
            }
            m.appendTail(result);
            return result.toString();
        }

        return formula;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CustomFormulaSyntax && customSyntaxes.equals(((CustomFormulaSyntax) o).customSyntaxes) && defaultSyntax.equals(((CustomFormulaSyntax) o).defaultSyntax) && params.equals(((CustomFormulaSyntax) o).params);

    }

    @Override
    public int hashCode() {
        return 31 * (31 * defaultSyntax.hashCode() + customSyntaxes.hashCode()) + params.hashCode();
    }
}
