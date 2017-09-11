package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public class CompileOptions<V> extends TwinImmutableObject {

    public final SQLSyntax syntax;
    public final LimitOptions limit;
    public final SubQueryContext subcontext;
    public final boolean recursive;
    public final boolean noInline;
    public final ImMap<V, Type> castTypes;
    
    public final boolean needDebugInfo; // если true, в CompiledQuery.sql.debugInfo будет заполнен

    public CompileOptions(SQLSyntax syntax, LimitOptions limit, SubQueryContext subcontext, boolean recursive, boolean noInline, ImMap<V, Type> castTypes, boolean needDebugInfo) {
        this.syntax = syntax;
        this.limit = limit;
        this.subcontext = subcontext;
        this.recursive = recursive;
        this.noInline = noInline;
        this.castTypes = castTypes;
        this.needDebugInfo = needDebugInfo;
    }

    public CompileOptions(SQLSyntax syntax, LimitOptions limit, SubQueryContext subcontext, boolean recursive, boolean needDebugInfo) {
        this(syntax, limit, subcontext, recursive, false, null, needDebugInfo);
    }

    public CompileOptions(SQLSyntax syntax, SubQueryContext subcontext, boolean recursive, boolean needDebugInfo) {
        this(syntax, LimitOptions.NOLIMIT, subcontext, recursive, needDebugInfo);
    }

    public CompileOptions(SQLSyntax syntax, LimitOptions limit, SubQueryContext subcontext, boolean needDebugInfo) {
        this(syntax, limit, subcontext, false, needDebugInfo);
    }

    public CompileOptions(SQLSyntax syntax, SubQueryContext subcontext, boolean needDebugInfo) {
        this(syntax, LimitOptions.NOLIMIT, subcontext, needDebugInfo);
    }

    // SELECT
    public CompileOptions(SQLSyntax syntax, LimitOptions limit, SubQueryContext subcontext) {
        this(syntax, limit, subcontext, false, false);
    }

    // DELETE
    public CompileOptions(SQLSyntax syntax, boolean noInline) {
        this(syntax, LimitOptions.NOLIMIT, SubQueryContext.EMPTY, false, noInline, null, false);
    }

    // UPDATE    
    public CompileOptions(SQLSyntax syntax, ImMap<V, Type> castTypes) {
        this(syntax, LimitOptions.NOLIMIT, SubQueryContext.EMPTY, false, false, castTypes, false);
    }

    // INSERT SELECT
    public CompileOptions(SQLSyntax syntax, ImMap<V, Type> castTypes, int top) {
        this(syntax, LimitOptions.get(top), SubQueryContext.EMPTY, false, false, castTypes, false);
    }

    // OUT, READ
    public CompileOptions(SQLSyntax syntax) {
        this(syntax, LimitOptions.NOLIMIT, SubQueryContext.EMPTY, false);
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return syntax.equals(((CompileOptions)o).syntax) && limit.equals(((CompileOptions)o).limit) && subcontext.equals(((CompileOptions)o).subcontext) && recursive == ((CompileOptions)o).recursive && noInline == ((CompileOptions)o).noInline && BaseUtils.nullEquals(castTypes, ((CompileOptions)o).castTypes) && needDebugInfo == ((CompileOptions)o).needDebugInfo;
    }

    public int immutableHashCode() {
        return ((syntax.hashCode() * 31 + limit.hashCode()) * 31 + subcontext.hashCode()) * 31 + (recursive ? 1 : 0) + (noInline ? 3 : 0) + (needDebugInfo ? 5 : 0) + BaseUtils.nullHash(castTypes);
    }

    public <MV> CompileOptions<MV> map(ImRevMap<V, MV> map) {
        if(castTypes == null)
            return (CompileOptions<MV>) this;
        return new CompileOptions<>(syntax, limit, subcontext, recursive, noInline, map.crossJoin(castTypes), needDebugInfo);
    }

    public CompileOptions<V> debug() {
        return new CompileOptions<>(syntax, limit, subcontext, recursive, noInline, castTypes, true);
    }
}
