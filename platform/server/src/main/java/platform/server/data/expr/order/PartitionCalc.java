package platform.server.data.expr.order;

import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.server.data.query.CompileOrder;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;

public class PartitionCalc extends PartitionToken {

    public static class Aggr {
        public String func;

        public ImList<PartitionToken> exprs;
        public ImOrderMap<PartitionToken, CompileOrder> orders;
        public ImSet<PartitionToken> partitions;

        public Aggr(String func, ImList<PartitionToken> exprs, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            this.func = func;
            this.exprs = exprs;
            this.orders = orders;
            this.partitions = partitions;
        }

        public Aggr(String func, ImList<PartitionToken> exprs, ImSet<PartitionToken> partitions) {
            this(func, exprs, MapFact.<PartitionToken, CompileOrder>EMPTYORDER(), partitions);
        }

        public Aggr(String func, ImSet<PartitionToken> partitions) {
            this(func, ListFact.<PartitionToken>EMPTY(), partitions);
        }

        public Aggr(String func, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            this(func, ListFact.<PartitionToken>EMPTY(), orders, partitions);
        }

        public String getSource(ImMap<PartitionToken, String> sources, SQLSyntax syntax) {
            return "(" + func + "(" + exprs.map(sources).toString(",") + ") OVER ("+BaseUtils.toString(" ",
                    BaseUtils.clause("PARTITION BY ",partitions.map(sources).toString(",")) +
                    BaseUtils.clause("ORDER BY ", Query.stringOrder(orders.map(sources), syntax))) + ")" + ")";
        }
    }

    public final String formula;
    public final ImMap<String, PartitionToken> params;
    public final ImMap<String, Aggr> aggrParams;

    @Override
    public String getSource(ImMap<PartitionToken, String> sources, SQLSyntax syntax) {
        String sourceString = formula;
        for(int i=0,size=params.size();i<size;i++)
            sourceString = sourceString.replace(params.getKey(i), sources.get(params.getValue(i)));
        for(int i=0,size=aggrParams.size();i<size;i++)
            sourceString = sourceString.replace(aggrParams.getKey(i), aggrParams.getValue(i).getSource(sources, syntax));
         return "("+sourceString+")";
    }

    public PartitionCalc(String formula, Aggr aggr, PartitionToken... listParams) {
        this(formula, listParams, aggr);
    }

    public PartitionCalc(String formula, PartitionToken[] listParams, Aggr... listAggrParams) {
        this.formula = formula;

        MExclMap<String, PartitionToken> mParams = MapFact.mExclMap(listParams.length); // массивы
        for(int i=0;i<listParams.length;i++)
            mParams.exclAdd("prm" + (i + 1), listParams[i]);
        params = mParams.immutable();

        MExclMap<String, Aggr> mAggrParams = MapFact.mExclMap(listAggrParams.length); // массивы
        for(int i=0;i<listAggrParams.length;i++)
            mAggrParams.exclAdd("prm" + (listParams.length + i + 1), listAggrParams[i]);
        aggrParams = mAggrParams.immutable();

        for(PartitionToken token : params.valueIt())
            token.addNext(this);
        for(Aggr aggrParam : aggrParams.valueIt()) {
            for(PartitionToken token : aggrParam.exprs)
                token.addNext(this);
            for(PartitionToken token : aggrParam.orders.keyIt())
                token.addNext(this);
            for(PartitionToken token : aggrParam.partitions)
                token.addNext(this);
        }
    }

    public PartitionCalc(Aggr aggr) {
        this("prm1", new PartitionToken[]{}, aggr);
    }

    public int getLevel() {
        int level = 0;
        for(PartitionToken param : params.valueIt())
            level = BaseUtils.max(level, param.getLevel());
        return level + 1;
    }
}
