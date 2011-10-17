package platform.server.data.expr.order;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;

import java.util.*;

public class OrderCalc extends OrderToken {

    public static class Aggr {
        public String func;

        public List<OrderToken> exprs;
        public OrderedMap<OrderToken, Boolean> orders;
        public Set<OrderToken> partitions;

        public Aggr(String func, List<OrderToken> exprs, OrderedMap<OrderToken, Boolean> orders, Set<OrderToken> partitions) {
            this.func = func;
            this.exprs = exprs;
            this.orders = orders;
            this.partitions = partitions;
        }

        public Aggr(String func, List<OrderToken> exprs, Set<OrderToken> partitions) {
            this(func, exprs, new OrderedMap<OrderToken, Boolean>(), partitions);
        }

        public Aggr(String func, Set<OrderToken> partitions) {
            this(func, new ArrayList<OrderToken>(), partitions);
        }

        public Aggr(String func, OrderedMap<OrderToken, Boolean> orders, Set<OrderToken> partitions) {
            this(func, new ArrayList<OrderToken>(), orders, partitions);
        }

        public String getSource(Map<OrderToken, String> sources, SQLSyntax syntax) {
            return "(" + func + "(" + BaseUtils.toString(BaseUtils.mapList(exprs, sources), ",") + ") OVER ("+BaseUtils.toString(" ",
                    BaseUtils.clause("PARTITION BY ",BaseUtils.toString(BaseUtils.filterKeys(sources, partitions).values(),",")) +
                    BaseUtils.clause("ORDER BY ", Query.stringOrder(BaseUtils.mapOrder(orders, sources), syntax))) + ")" + ")";
        }
    }

    public final String formula;
    public final Map<String, OrderToken> params;
    public final Map<String, Aggr> aggrParams;

    @Override
    public String getSource(Map<OrderToken, String> sources, SQLSyntax syntax) {
        String sourceString = formula;
        for(Map.Entry<String, OrderToken> prm : params.entrySet())
            sourceString = sourceString.replace(prm.getKey(), sources.get(prm.getValue()));
        for(Map.Entry<String, Aggr> prm : aggrParams.entrySet())
            sourceString = sourceString.replace(prm.getKey(), prm.getValue().getSource(sources, syntax));
         return "("+sourceString+")";
    }

    public OrderCalc(String formula, Aggr aggr, OrderToken... listParams) {
        this(formula, listParams, aggr);
    }

    public OrderCalc(String formula, OrderToken[] listParams, Aggr... listAggrParams) {
        this.formula = formula;

        params = new HashMap<String, OrderToken>();
        for(int i=0;i<listParams.length;i++)
            params.put("prm"+(i+1), listParams[i]);

        aggrParams = new HashMap<String, Aggr>();
        for(int i=0;i<listAggrParams.length;i++)
            aggrParams.put("prm"+(listParams.length+i+1), listAggrParams[i]);

        for(OrderToken param : params.values())
            param.next.add(this);
        for(Aggr aggrParam : aggrParams.values()) {
            for(OrderToken token : aggrParam.exprs)
                token.next.add(this);
            for(OrderToken token : aggrParam.orders.keySet())
                token.next.add(this);
            for(OrderToken token : aggrParam.partitions)
                token.next.add(this);
        }
    }

    public OrderCalc(Aggr aggr) {
        this("prm1", new OrderToken[]{}, aggr);
    }

    public int getLevel() {
        int level = 0;
        for(OrderToken param : params.values())
            level = BaseUtils.max(level, param.getLevel());
        return level + 1;
    }
}
