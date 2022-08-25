package lsfusion.server.data.expr.where.classes.data;

import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.where.Where;
import lsfusion.server.physics.admin.Settings;

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

    public static String getPrefixSearchVector(SQLSyntax syntax, String source, String language) {
        return "to_tsvector('" + language + "', " + source + ")";
    }
    public static String getPrefixSearchQuery(SQLSyntax syntax, String source, String language) {
        return syntax.getPrefixSearchQuery() + "('" + language + "', " + source + ", '" + Settings.get().getMatchSearchSeparator() + "')";
    }
    public static String getMatch(SQLSyntax syntax, String search, String match, String language) {
        return getPrefixSearchVector(syntax, search, language) + " @@ " + getPrefixSearchQuery(syntax, match, language);
    }
    public static String getRank(SQLSyntax syntax, String search, String match, String language) {
        return "ts_rank(" + getPrefixSearchVector(syntax, search, language) + "," + getPrefixSearchQuery(syntax, match, language) + ")";
    }
    public static String getHighlight(SQLSyntax syntax, String search, String match, String language) {
        return "ts_headline('" + language + "'," + search + "," + getPrefixSearchQuery(syntax, match, language) + ")";
    }

    @Override
    protected String getBaseSource(CompileSource compile) {
        String source = operator1.getSource(compile);
        String match = operator2.getSource(compile);

        String language = Settings.get().getFilterMatchLanguage();
        String matchString = getMatch(compile.syntax, source, match, language);

        String likeString = source + (" " + compile.syntax.getInsensitiveLike() + " ")
                + "(" + ("'%' " + compile.syntax.getStringConcatenate() + " ") + match + (" " + compile.syntax.getStringConcatenate() + " '%'") + ")";

        return "(" + matchString + " OR " + likeString + ")";
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(checkEquals(operator1, operator2))
            return operator1.getWhere();
        return create(operator1, operator2, new MatchWhere(operator1, operator2));
    }
}
