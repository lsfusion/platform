package platform.server.data.query.exprs;

import platform.server.data.query.Translator;
import platform.server.data.query.MapJoinEquals;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.exprs.cases.ExprCase;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.types.Type;
import platform.server.where.Where;

import java.util.Map;

import net.jcip.annotations.Immutable;

// абстрактный класс выражений

abstract public class SourceExpr extends AbstractSourceJoin {

    public abstract Type getType();

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    public abstract SourceExpr translate(Translator translator);

    public static <K> boolean containsNull(Map<K,? extends SourceExpr> Map) {
        for(SourceExpr CaseExpr : Map.values())
            if(CaseExpr instanceof NullExpr) return true;
        return false;
    }

    // возвращает Where на notNull
    private Where where=null;
    public Where getWhere() {
        if(where==null)
            where = calculateWhere();
        return where;
    }
    abstract protected Where calculateWhere();

    // получает список ExprCase'ов
    public abstract ExprCaseList getCases();

    // проталкивает условие внутрь
    public SourceExpr and(Where where) {
        ExprCaseList translatedCases = new ExprCaseList();
        for(ExprCase exprCase : getCases())
            translatedCases.add(exprCase.where.and(where),exprCase.data);
        return translatedCases.getExpr(getType());
    }

    public abstract SourceExpr followFalse(Where where);

    // для кэша
    public abstract boolean equals(SourceExpr expr, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins);
}

