package lsfusion.server.data.query;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.data.sql.SQLSyntax;

public class CompileOptions extends TwinImmutableObject {

    public final SQLSyntax syntax;
    public final LimitOptions limit;
    public final SubQueryContext subcontext;
    public final boolean recursive;
    public final boolean noInline;

    public CompileOptions(SQLSyntax syntax, LimitOptions limit, SubQueryContext subcontext, boolean recursive, boolean noInline) {
        this.syntax = syntax;
        this.limit = limit;
        this.subcontext = subcontext;
        this.recursive = recursive;
        this.noInline = noInline;
    }

    public CompileOptions(SQLSyntax syntax, boolean noInline) {
        this(syntax, LimitOptions.NOLIMIT, SubQueryContext.EMPTY, false, noInline);
    }

    public CompileOptions(SQLSyntax syntax, LimitOptions limit, SubQueryContext subcontext, boolean recursive) {
        this(syntax, limit, subcontext, recursive, false);
    }

    public CompileOptions(SQLSyntax syntax, SubQueryContext subcontext, boolean recursive) {
        this(syntax, LimitOptions.NOLIMIT, subcontext, recursive);
    }

    public CompileOptions(SQLSyntax syntax, LimitOptions limit, SubQueryContext subcontext) {
        this(syntax, limit, subcontext, false);
    }

    public CompileOptions(SQLSyntax syntax, SubQueryContext subcontext) {
        this(syntax, LimitOptions.NOLIMIT, subcontext);
    }

    public CompileOptions(SQLSyntax syntax, LimitOptions limit) {
        this(syntax, limit, SubQueryContext.EMPTY);
    }

    public CompileOptions(SQLSyntax syntax) {
        this(syntax, LimitOptions.NOLIMIT);
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return syntax.equals(((CompileOptions)o).syntax) && limit.equals(((CompileOptions)o).limit) && subcontext.equals(((CompileOptions)o).subcontext) && recursive == ((CompileOptions)o).recursive && noInline == ((CompileOptions)o).noInline;
    }

    public int immutableHashCode() {
        return ((syntax.hashCode() * 31 + limit.hashCode()) * 31 + subcontext.hashCode()) * 31 + (recursive ? 1 : 0) + (noInline ? 3 : 0);
    }
}
