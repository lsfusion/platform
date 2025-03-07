package lsfusion.server.data.expr.query.order;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.compile.CompileOrder;
import lsfusion.server.data.sql.syntax.SQLSyntax;

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
            this(func, exprs, MapFact.EMPTYORDER(), partitions);
        }

        public Aggr(String func, ImSet<PartitionToken> partitions) {
            this(func, ListFact.EMPTY(), partitions);
        }

        public Aggr(String func, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            this(func, ListFact.EMPTY(), orders, partitions);
        }

        public String getSource(ImMap<PartitionToken, String> sources, SQLSyntax syntax) {
            String exprs = this.exprs.mapList(sources).toString(",");
            if(func.equals(PartitionType.SELECT_FUNC)) // optimization
                return exprs;

            return "(" + func + "(" + exprs + ") OVER ("+BaseUtils.toString(" ",
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
//        if(hasMin) {
//            assert resultType != null;
//            sourceString = sourceString.replace(PartitionType.rType, resultType.getDB(syntax, typeEnv));
//        }
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
