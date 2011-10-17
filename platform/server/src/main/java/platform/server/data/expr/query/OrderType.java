package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.order.OrderCalc;
import platform.server.data.expr.order.OrderParam;
import platform.server.data.expr.order.OrderToken;
import platform.server.data.expr.where.Case;
import platform.server.data.sql.SQLSyntax;

import java.util.*;

public enum OrderType implements AggrType {
    SUM, DISTR_CUM_PROPORTION, DISTR_RESTRICT, DISTR_RESTRICT_OVER, PREVIOUS;

    public static <K> Set<K> getSet(List<K> exprs, OrderedMap<K, Boolean> orders, Collection<K> partitions) {
        Set<K> result = new HashSet<K>();
        result.addAll(exprs);
        result.addAll(orders.keySet());
        result.addAll(partitions);
        return result;
    }

    // вообще первый параметр OrderParam, но не хочется с generics'ами играться
    public OrderCalc createAggr(Map<OrderToken, String> tokens, List<String> sourceExprs, OrderedMap<String, Boolean> sourceOrders, Set<String> sourcePartitions) {
        Map<String, OrderParam> params = new HashMap<String, OrderParam>();
        for(String expr : getSet(sourceExprs, sourceOrders, sourcePartitions))
            params.put(expr, new OrderParam());
        tokens.putAll(BaseUtils.reverse(params));

        List<OrderToken> exprs = BaseUtils.<String, OrderToken>mapList(sourceExprs, params);
        OrderedMap<OrderToken, Boolean> orders = BaseUtils.<String, Boolean, OrderToken>mapOrder(sourceOrders, params);
        Set<OrderToken> partitions = new HashSet<OrderToken>(BaseUtils.filterKeys(params, sourcePartitions).values());

        switch (this) {
            case SUM:
            case PREVIOUS:
                return new OrderCalc(new OrderCalc.Aggr(this==PREVIOUS?"lag":toString(), exprs, orders, partitions));
            case DISTR_CUM_PROPORTION: // exprs : 1-й - пропорционально чему, 2-й что
                // 1-й пробег высчитываем огругленную и часть, и первую запись на которую ра
                OrderCalc.Aggr part = new OrderCalc.Aggr("SUM", Collections.singletonList(exprs.get(0)), partitions);
                OrderCalc round = new OrderCalc("ROUND(CAST((prm1*prm2/prm3) AS numeric),0)", part, exprs.get(0), exprs.get(1));
                OrderCalc number = new OrderCalc(new OrderCalc.Aggr("ROW_NUMBER", orders, partitions));
                // 2-й пробег - результат
                OrderCalc.Aggr totRound = new OrderCalc.Aggr("SUM", Collections.<OrderToken>singletonList(round), partitions);
                return new OrderCalc("prm1 + (CASE WHEN prm2=1 THEN (prm3-prm4) ELSE 0 END)", totRound, round, number, exprs.get(1));
        }
        throw new RuntimeException("not supported");
    }

    public boolean isSelect() {
        return this==PREVIOUS;
    }

    public boolean canBeNull() { // может возвращать null если само выражение не null
        return this==PREVIOUS;
    }
}
