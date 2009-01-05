package platformlocal;

import java.util.*;

// абстрактный класс выражений
abstract class SourceExpr implements SourceJoin {

    abstract Type getType();

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    abstract SourceExpr translate(Translator Translator);

    boolean isNull() {return false;}
    static <K> boolean containsNull(Map<K,? extends SourceExpr> Map) {
        for(SourceExpr CaseExpr : Map.values())
            if(CaseExpr.isNull()) return true;
        return false;
    }

    // возвращает Where на notNull
    abstract Where getWhere();

    // получает список ExprCase'ов
    abstract ExprCaseList getCases();

    SourceExpr compile(Where QueryWhere) {
        if(1==2 && QueryWhere.and(getWhere()).isFalse())
            return new ValueExpr(null,getType());
        else
            return this;
    }

    // для кэша
    abstract boolean equals(SourceExpr Expr, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres);

    abstract int hash();
}

abstract class AndExpr extends SourceExpr {

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    abstract boolean follow(DataWhere Where);

    public void fillJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        fillAndJoinWheres(Joins,AndWhere.and(getWhere()));
    }

    abstract protected void fillAndJoinWheres(MapWhere<JoinData> Joins,Where AndWhere);
}

