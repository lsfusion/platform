package lsfusion.server.data.expr.order;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.query.CompileOrder;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

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
            return "(" + func + "(" + exprs.mapList(sources).toString(",") + ") OVER ("+BaseUtils.toString(" ",
                    BaseUtils.clause("PARTITION BY ",partitions.map(sources).toString(",")) +
                    BaseUtils.clause("ORDER BY ", Query.stringOrder(orders.map(sources), syntax))) + ")" + ")";
        }
    }

    public final String formula;
    public final boolean hasMin;
    public final ImMap<String, PartitionToken> params;
    public final ImMap<String, Aggr> aggrParams;

    @Override
    public String getSource(ImMap<PartitionToken, String> sources, SQLSyntax syntax, Type resultType, TypeEnvironment typeEnv) {
        String sourceString = formula;
        for(int i=0,size=params.size();i<size;i++)
            sourceString = sourceString.replace(params.getKey(i), sources.get(params.getValue(i)));
        for(int i=0,size=aggrParams.size();i<size;i++)
            sourceString = sourceString.replace(aggrParams.getKey(i), aggrParams.getValue(i).getSource(sources, syntax));
        if(hasMin) {
            assert resultType != null;
            sourceString = sourceString.replace(PartitionType.rType, resultType.getDB(syntax, typeEnv));
        }
        return "("+sourceString+")";
    }

    public PartitionCalc(String formula, Aggr aggr, PartitionToken... listParams) {
        this(formula, false, aggr, listParams);
    }

    public PartitionCalc(String formula, boolean hasMin, Aggr aggr, PartitionToken... listParams) {
        this(formula, hasMin, listParams, aggr);
    }

    public PartitionCalc(String formula, boolean hasMin, PartitionToken[] listParams, Aggr... listAggrParams) {
        this.formula = formula;
        this.hasMin = hasMin;

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
        this("prm1", false, new PartitionToken[]{}, aggr);
    }

    public int getLevel() {
        int level = 0;
        for(PartitionToken param : params.valueIt())
            level = BaseUtils.max(level, param.getLevel());
        return level + 1;
    }
}
