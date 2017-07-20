package lsfusion.server.data.expr.formula;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.sql.SQLSyntax;

public class CustomFormulaSyntax {
    
    private final String defaultSyntax;    
    private final ImMap<SQLSyntaxType, String> customSyntaxes;
    
    public String getDefaultSyntax() {
        if(defaultSyntax.isEmpty())
            return customSyntaxes.getValue(0);
        return defaultSyntax;
    }

    public CustomFormulaSyntax(String defaultSyntax) {
        this(defaultSyntax, MapFact.<SQLSyntaxType, String>EMPTY());
    }

    public CustomFormulaSyntax(String defaultSyntax, ImMap<SQLSyntaxType, String> customSyntaxes) {
        this.defaultSyntax = defaultSyntax;
        this.customSyntaxes = customSyntaxes;
    }

    public String getFormula(SQLSyntaxType type) {
        String formula = customSyntaxes.get(type);
        if(formula != null)
            return formula;
            
        return defaultSyntax;            
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof CustomFormulaSyntax && customSyntaxes.equals(((CustomFormulaSyntax) o).customSyntaxes) && defaultSyntax.equals(((CustomFormulaSyntax) o).defaultSyntax);

    }

    @Override
    public int hashCode() {
        return 31 * defaultSyntax.hashCode() + customSyntaxes.hashCode();
    }
}
