package platform.server.data.query.exprs;

import platform.server.data.query.SourceJoin;
import platform.server.data.query.Translator;
import platform.server.data.query.MapJoinEquals;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;

// абстрактный класс выражений
abstract public class SourceExpr implements SourceJoin {

    public abstract Type getType();

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    public abstract SourceExpr translate(Translator translator);

    public static <K> boolean containsNull(Map<K,? extends SourceExpr> Map) {
        for(SourceExpr CaseExpr : Map.values())
            if(CaseExpr instanceof NullExpr) return true;
        return false;
    }

    // возвращает Where на notNull
    Where where=null;
    public Where getWhere() {
        if(where==null)
            where = calculateWhere();
        return where;
    }
    abstract Where calculateWhere();

    // получает список ExprCase'ов
    abstract ExprCaseList getCases();

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

    boolean hashed = false;
    int hash;
    public int hash() {
        if(!hashed) {
            hash = DataWhereSet.hash(getHash());
            hashed = true;
        }
        return hash;
    }
    abstract int getHash();
}

