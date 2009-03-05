package platform.server.data.query;

import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.wheres.JoinWhere;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

// нужен для кэширования запросов
public class MapJoinEquals {

    public boolean equals(JoinExpr expr1,JoinExpr expr2) {
        return exprs.get(expr1).contains(expr2);
    }

    public boolean equals(JoinWhere where1,JoinWhere where2) {
        return wheres.get(where1).contains(where2);
    }

    Map<JoinExpr,Set<JoinExpr>> exprs = new HashMap<JoinExpr,Set<JoinExpr>>();
    public void put(JoinExpr expr1,JoinExpr expr2) {
        Set<JoinExpr> exprSet = exprs.get(expr1);
        if(exprSet==null) {
            exprSet = new HashSet<JoinExpr>();
            exprs.put(expr1,exprSet);
        }
        exprSet.add(expr2);
    }

    Map<JoinWhere,Set<JoinWhere>> wheres = new HashMap<JoinWhere,Set<JoinWhere>>();
    public void put(JoinWhere where1,JoinWhere where2) {
        Set<JoinWhere> whereSet = wheres.get(where1);
        if(whereSet==null) {
            whereSet = new HashSet<JoinWhere>();
            wheres.put(where1,whereSet);
        }
        whereSet.add(where2);
    }
}
