package platformlocal;

import java.util.*;

// абстрактный класс выражений
abstract class SourceExpr implements SourceJoin {

    abstract Type getType();

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    abstract SourceExpr translate(Translator translator);

    static <K> boolean containsNull(Map<K,? extends SourceExpr> Map) {
        for(SourceExpr CaseExpr : Map.values())
            if(CaseExpr instanceof NullExpr) return true;
        return false;
    }

    // возвращает Where на notNull
    abstract Where getWhere();

    // получает список ExprCase'ов
    abstract ExprCaseList getCases();

    // проталкивает условие внутрь
    SourceExpr and(Where where) {
        ExprCaseList translatedCases = new ExprCaseList();
        for(ExprCase exprCase : getCases())
            translatedCases.add(exprCase.where.and(where),exprCase.data);
        return translatedCases.getExpr(getType());
    }

    abstract SourceExpr followFalse(Where where);

    // для кэша
    abstract boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres);

    boolean Hashed = false;
    int Hash;
    int hash() {
        if(!Hashed) {
            Hash = getHash();
            Hashed = true;
        }
        return Hash;
    }
    abstract int getHash();
}

abstract class AndExpr extends SourceExpr {

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    abstract DataWhereSet getFollows();

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere.and(getWhere()));
    }


    abstract protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    SourceExpr followFalse(Where where) {
        if(getWhere().means(where))
            return getType().getExpr(null);
        else
            return this;
    }
}

