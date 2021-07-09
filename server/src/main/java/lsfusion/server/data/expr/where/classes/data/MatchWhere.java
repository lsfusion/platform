package lsfusion.server.data.expr.where.classes.data;

import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.physics.admin.Settings;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MatchWhere extends BinaryWhere<MatchWhere> {

    private MatchWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    protected MatchWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new MatchWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.MATCH;
    }

    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return (operator1.hashOuter(hashContext) * 31 + operator2.hashOuter(hashContext)) * 31;
    }

    protected String getCompareSource(CompileSource compile) {
        throw new RuntimeException("not supported");
    }

    @Override
    protected String getBaseSource(CompileSource compile) {
        List<String> languages = Arrays.asList(Settings.get().getFilterMatchLanguages().split(","));
        Type type = operator1.getType(compile.keyType);
        String source = operator1.getSource(compile);
        String match = operator2.getSource(compile);
        String matchString = languages.stream().map(language -> "to_tsvector('" + language + "', " + source + ") @@ " + compile.syntax.getWebSearchToTSQuery() + "('" + language + "', " + match + ")").collect(Collectors.joining(" OR "));
        String likeString = source + (type instanceof StringClass && ((StringClass) type).caseInsensitive ? " " + compile.syntax.getInsensitiveLike() + " " : " LIKE ")
                + "(" + ("'%' " + compile.syntax.getStringConcatenate() + " ") + match + (" " + compile.syntax.getStringConcatenate() + " '%'") + ")";

        return "((" + matchString + ") OR " + likeString + ")";
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(checkEquals(operator1, operator2))
            return operator1.getWhere();
        return create(operator1, operator2, new MatchWhere(operator1, operator2));
    }
}
