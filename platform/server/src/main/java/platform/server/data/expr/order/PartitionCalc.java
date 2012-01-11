package platform.server.data.expr.order;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;

import java.util.*;

public class PartitionCalc extends PartitionToken {

    public static class Aggr {
        public String func;

        public List<PartitionToken> exprs;
        public OrderedMap<PartitionToken, Boolean> orders;
        public Set<PartitionToken> partitions;

        public Aggr(String func, List<PartitionToken> exprs, OrderedMap<PartitionToken, Boolean> orders, Set<PartitionToken> partitions) {
            this.func = func;
            this.exprs = exprs;
            this.orders = orders;
            this.partitions = partitions;
        }

        public Aggr(String func, List<PartitionToken> exprs, Set<PartitionToken> partitions) {
            this(func, exprs, new OrderedMap<PartitionToken, Boolean>(), partitions);
        }

        public Aggr(String func, Set<PartitionToken> partitions) {
            this(func, new ArrayList<PartitionToken>(), partitions);
        }

        public Aggr(String func, OrderedMap<PartitionToken, Boolean> orders, Set<PartitionToken> partitions) {
            this(func, new ArrayList<PartitionToken>(), orders, partitions);
        }

        public String getSource(Map<PartitionToken, String> sources, SQLSyntax syntax) {
            return "(" + func + "(" + BaseUtils.toString(BaseUtils.mapList(exprs, sources), ",") + ") OVER ("+BaseUtils.toString(" ",
                    BaseUtils.clause("PARTITION BY ",BaseUtils.toString(BaseUtils.filterKeys(sources, partitions).values(),",")) +
                    BaseUtils.clause("ORDER BY ", Query.stringOrder(BaseUtils.mapOrder(orders, sources), syntax))) + ")" + ")";
        }
    }

    public final String formula;
    public final Map<String, PartitionToken> params;
    public final Map<String, Aggr> aggrParams;

    @Override
    public String getSource(Map<PartitionToken, String> sources, SQLSyntax syntax) {
        String sourceString = formula;
        for(Map.Entry<String, PartitionToken> prm : params.entrySet())
            sourceString = sourceString.replace(prm.getKey(), sources.get(prm.getValue()));
        for(Map.Entry<String, Aggr> prm : aggrParams.entrySet())
            sourceString = sourceString.replace(prm.getKey(), prm.getValue().getSource(sources, syntax));
         return "("+sourceString+")";
    }

    public PartitionCalc(String formula, Aggr aggr, PartitionToken... listParams) {
        this(formula, listParams, aggr);
    }

    public PartitionCalc(String formula, PartitionToken[] listParams, Aggr... listAggrParams) {
        this.formula = formula;

        params = new HashMap<String, PartitionToken>();
        for(int i=0;i<listParams.length;i++)
            params.put("prm"+(i+1), listParams[i]);

        aggrParams = new HashMap<String, Aggr>();
        for(int i=0;i<listAggrParams.length;i++)
            aggrParams.put("prm"+(listParams.length+i+1), listAggrParams[i]);

        for(PartitionToken param : params.values())
            param.next.add(this);
        for(Aggr aggrParam : aggrParams.values()) {
            for(PartitionToken token : aggrParam.exprs)
                token.next.add(this);
            for(PartitionToken token : aggrParam.orders.keySet())
                token.next.add(this);
            for(PartitionToken token : aggrParam.partitions)
                token.next.add(this);
        }
    }

    public PartitionCalc(Aggr aggr) {
        this("prm1", new PartitionToken[]{}, aggr);
    }

    public int getLevel() {
        int level = 0;
        for(PartitionToken param : params.values())
            level = BaseUtils.max(level, param.getLevel());
        return level + 1;
    }
}
