package platform.server.data.query;

import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.JoinWhere;
import platform.base.BaseUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// нужен для кэширования запросов
public class MapContext {

    public Map<ValueExpr,ValueExpr> values;
    public Map<KeyExpr, KeyExpr> keys;

    public MapContext(Map<ValueExpr,ValueExpr> iValues,Map<KeyExpr,KeyExpr> iKeys) {
        values = iValues;
        keys = iKeys;
    }

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

    public <EV,V> Map<V,EV> equalProps(Map<V, ? extends SourceExpr> props, Map<EV, ? extends SourceExpr> equalProps) {
        if(props.size()!=equalProps.size()) return null;

        Map<V,EV> mapProps = new HashMap<V,EV>();
        for(Map.Entry<V,? extends SourceExpr> mapProperty : props.entrySet()) {
            EV mapQuery = null;
            for(Map.Entry<EV,? extends SourceExpr> mapQueryProperty : equalProps.entrySet())
                if(!mapProps.containsValue(mapQueryProperty.getKey()) &&
                    mapProperty.getValue().equals(mapQueryProperty.getValue(), this)) {
                    mapQuery = mapQueryProperty.getKey();
                    break;
                }
            if(mapQuery==null) return null;
            mapProps.put(mapProperty.getKey(),mapQuery);
        }
        return mapProps;
    }

    public <V> boolean equals(Map<V, ? extends SourceExpr> props, Map<V, ? extends SourceExpr> equalProps) {
        if(props.size()!=equalProps.size()) return false;

        for(Map.Entry<V,? extends SourceExpr> prop : props.entrySet())
            if(!prop.getValue().equals(equalProps.get(prop.getKey()),this))
                return false; 

        return true;
    }
}
