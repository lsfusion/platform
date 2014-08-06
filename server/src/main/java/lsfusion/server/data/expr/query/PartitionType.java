package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.order.PartitionCalc;
import lsfusion.server.data.expr.order.PartitionParam;
import lsfusion.server.data.expr.order.PartitionToken;
import lsfusion.server.data.query.CompileOrder;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public enum PartitionType implements AggrType {
    SUM, DISTR_CUM_PROPORTION, DISTR_RESTRICT, DISTR_RESTRICT_OVER, PREVIOUS;

    public static <K> ImSet<K> getSet(ImList<K> exprs, ImOrderMap<K, CompileOrder> orders, ImSet<K> partitions) {
        return SetFact.add(exprs.toOrderSet().getSet(), orders.keys(), partitions);
    }

    public static final String rType = "DFF3434FDFDFD";  
    
    // вообще первый параметр PartitionParam, но не хочется с generics'ами играться
    public PartitionCalc createAggr(MExclMap<PartitionToken, String> mTokens, ImList<String> sourceExprs, ImOrderMap<String, CompileOrder> sourceOrders, ImSet<String> sourcePartitions, SQLSyntax syntax, Type type, TypeEnvironment typeEnv) {
        ImSet<String> paramNames = getSet(sourceExprs, sourceOrders, sourcePartitions);
        ImRevMap<String, PartitionParam> params = paramNames.mapRevValues(new GetStaticValue<PartitionParam>() {
            public PartitionParam getMapValue() {
                return new PartitionParam();
            }});
        mTokens.exclAddAll(params.reverse());

        ImRevMap<String, PartitionToken> castParams = BaseUtils.immutableCast(params);
        ImList<PartitionToken> exprs = sourceExprs.mapList(castParams);
        ImOrderMap<PartitionToken, CompileOrder> orders = sourceOrders.map(castParams);
        ImSet<PartitionToken> partitions = sourcePartitions.mapRev(castParams);

        String distrMin = null;
        if(this == DISTR_RESTRICT || this == DISTR_RESTRICT_OVER) {
            String prm1 = "prm1";
            String prm2 = "prm1+prm2-prm3";
            if(syntax.noMaxImplicitCast()) {
                String rType = type.getDB(syntax, typeEnv);
                prm1 = "CAST(" + prm1 + " AS " + rType + ")";
                prm2 = "CAST(" + prm2 + " AS " + rType + ")";  
            }
            distrMin = syntax.getMaxMin(false, prm1, prm2, type, typeEnv);
        }

        switch (this) {
            case SUM:
            case PREVIOUS:
                return new PartitionCalc(new PartitionCalc.Aggr(this==PREVIOUS?"lag":toString(), exprs, orders, partitions));
            case DISTR_CUM_PROPORTION: // exprs : 1-й - пропорционально чему, 2-й что
                // 1-й пробег высчитываем огругленную и часть, и первую запись на которую ра
                PartitionCalc.Aggr part = new PartitionCalc.Aggr("SUM", ListFact.singleton(exprs.get(0)), partitions);
                PartitionCalc round = new PartitionCalc("ROUND(CAST((prm1*prm2/prm3) AS numeric),0)", part, exprs.get(0), exprs.get(1));
                PartitionCalc number = new PartitionCalc(new PartitionCalc.Aggr("ROW_NUMBER", orders, partitions));
                // 2-й пробег - результат
                PartitionCalc.Aggr totRound = new PartitionCalc.Aggr("SUM", ListFact.<PartitionToken>singleton(round), partitions);
                return new PartitionCalc("prm1 + (CASE WHEN prm2=1 THEN (prm3-prm4) ELSE 0 END)", totRound, round, number, exprs.get(1));
            case DISTR_RESTRICT: // exprs : 1-й - ограничение, 2-й что
                // 1-й пробег высчитываем накопленные ограничения
                PartitionCalc.Aggr sumRestr = new PartitionCalc.Aggr("SUM", ListFact.singleton(exprs.get(0)), orders, partitions);
                return new PartitionCalc("(" + syntax.getAndExpr("prm2>(prm3-prm1)", distrMin,  type, typeEnv) + ")", sumRestr, exprs.get(0), exprs.get(1));
            case DISTR_RESTRICT_OVER: // exprs : 1-й - ограничение, 2-й что
                // 1-й пробег высчитываем накопленные ограничения
                PartitionCalc.Aggr sumTot = new PartitionCalc.Aggr("SUM", ListFact.singleton(exprs.get(0)), partitions);
                PartitionCalc.Aggr sumRestrOver = new PartitionCalc.Aggr("SUM", ListFact.singleton(exprs.get(0)), orders, partitions);
                PartitionCalc.Aggr restrNumber = new PartitionCalc.Aggr("ROW_NUMBER", orders, partitions);
                return new PartitionCalc("(" + syntax.getAndExpr("prm2>(prm3-prm1)", "(" + distrMin + "+(CASE WHEN prm5=1 AND prm2>prm4 THEN prm2-prm4 ELSE 0 END))", type, typeEnv) + ")", new PartitionToken[]{exprs.get(0), exprs.get(1)}, sumRestrOver, sumTot, restrNumber);
        }
        throw new RuntimeException("not supported");
    }

    public boolean isSelect() {
        return this==PREVIOUS || this==DISTR_RESTRICT || this==DISTR_RESTRICT_OVER;
    }

    public boolean canBeNull() { // может возвращать null если само выражение не null
        return this==PREVIOUS || this==DISTR_RESTRICT || this==DISTR_RESTRICT_OVER;
    }

    public boolean isSelectNotInWhere() { // в общем то оптимизационная вещь потом можно убрать
//        assert isSelect();
        return false;
    }

    public Type getType(Type exprType) {
        return exprType;
    }

    public int getMainIndex() {
        return 0;
    }

    public Where getWhere(ImList<Expr> exprs) {
        return Expr.getWhere(exprs);
    }

    public Expr getMainExpr(ImList<Expr> exprs) {
        return exprs.get(0);
    }
    
    
}
