package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.server.data.expr.query.order.PartitionCalc;
import lsfusion.server.data.expr.query.order.PartitionParam;
import lsfusion.server.data.expr.query.order.PartitionToken;
import lsfusion.server.data.query.compile.CompileOrder;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class PartitionType implements AggrType {

    protected abstract PartitionCalc getPartitionCalc(SQLSyntax syntax, Type type, TypeEnvironment typeEnv, MExclSet<PartitionCalc> mCalcTokens, ImList<PartitionToken> exprs, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions);
    
    private final static Map<Integer, PartitionType> distrCumTypes = new HashMap<>();

    // converted to static methods to prevent class initialization deadlocks 
    public static PartitionType sum() {
        return SUM;
    }

    public static PartitionType previous() {
        return PREVIOUS;
    }

    public static PartitionType distrRestrict() {
        return DistrRestrPartitionType.DISTR_RESTRICT;
    }

    public static PartitionType distrRestrictOver() {
        return DistrRestrPartitionType.DISTR_RESTRICT_OVER;
    }

    public static PartitionType distrCumProportion() {
        return DISTR_CUM_PROPORTION;
    }

    private final static PartitionType SUM = new PartitionType() {
        protected PartitionCalc getPartitionCalc(SQLSyntax syntax, Type type, TypeEnvironment typeEnv, MExclSet<PartitionCalc> mCalcTokens, ImList<PartitionToken> exprs, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            return new PartitionCalc(new PartitionCalc.Aggr("SUM", exprs, orders, partitions));
        }
    };

    public final static PartitionType PREVIOUS = new PartitionType() {
        protected PartitionCalc getPartitionCalc(SQLSyntax syntax, Type type, TypeEnvironment typeEnv, MExclSet<PartitionCalc> mCalcTokens, ImList<PartitionToken> exprs, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            return new PartitionCalc(new PartitionCalc.Aggr("lag", exprs, orders, partitions));
        }
        public boolean canBeNull() {
            return true;
        }
        public boolean isSelect() {
            return true;
        }
    };

    public static class Custom extends PartitionType {
        public final String aggrFunc;

        public final DataClass dataClass;
        public final boolean valueNull;

        public Custom(String aggrFunc, DataClass dataClass, boolean valueNull) {
            this.aggrFunc = aggrFunc;

            this.dataClass = dataClass;
            this.valueNull = valueNull;
        }

        @Override
        protected PartitionCalc getPartitionCalc(SQLSyntax syntax, Type type, TypeEnvironment typeEnv, MExclSet<PartitionCalc> mCalcTokens, ImList<PartitionToken> exprs, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            return new PartitionCalc(new PartitionCalc.Aggr(aggrFunc, exprs, orders, partitions));
        }

        public Type getType(Type exprType) {
            if(dataClass != null)
                return dataClass;

            return super.getType(exprType);
        }
        public Stat getTypeStat(Stat typeStat, boolean forJoin) {
            if(dataClass != null)
                return dataClass.getTypeStat(forJoin);

            return super.getTypeStat(typeStat, forJoin);
        }

        public boolean canBeNull() {
            return valueNull;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof Custom && aggrFunc.equals(((Custom) o).aggrFunc)  && BaseUtils.nullEquals(dataClass, ((GroupType.Custom) o).dataClass) && valueNull == ((GroupType.Custom) o).valueNull;
        }

        public int hashCode() {
            return Objects.hash(aggrFunc, dataClass, valueNull);
        }
    }
    public static PartitionType CUSTOM(String aggrFunc, DataClass dataClass, boolean valueNull) {
        return new Custom(aggrFunc, dataClass, valueNull);
    }

    public final static PartitionType DISTR_CUM_PROPORTION = distrCumProportion(0); // кривовато немного, потом надо будет переделать
    
    public static PartitionType distrCumProportion(int round) {
        synchronized (distrCumTypes) {
            PartitionType type = distrCumTypes.get(round);
            if(type == null) {
                type = new DistrCumPartitionType(round);
                distrCumTypes.put(round, type);
            }
            return type;
        }
    }

    public static <K> ImSet<K> getSet(ImList<K> exprs, ImOrderMap<K, CompileOrder> orders, ImSet<K> partitions) {
        return SetFact.add(exprs.toOrderSet().getSet(), orders.keys(), partitions);
    }

    public static final String rType = "DFF3434FDFDFD";  
    
    // вообще первый параметр PartitionParam, но не хочется с generics'ами играться
    public PartitionCalc createAggr(MExclMap<PartitionToken, String> mTokens, ImList<String> sourceExprs, ImOrderMap<String, CompileOrder> sourceOrders, ImSet<String> sourcePartitions, SQLSyntax syntax, Type type, TypeEnvironment typeEnv) {
        ImSet<String> paramNames = getSet(sourceExprs, sourceOrders, sourcePartitions);
        ImRevMap<String, PartitionParam> params = paramNames.mapRevValues(PartitionParam::new);
        mTokens.exclAddAll(params.reverse());
        MExclSet<PartitionCalc> mCalcTokens = SetFact.mExclSet();

        ImRevMap<String, PartitionToken> castParams = BaseUtils.immutableCast(params);
        ImList<PartitionToken> exprs = sourceExprs.mapList(castParams);
        ImOrderMap<PartitionToken, CompileOrder> orders = sourceOrders.map(castParams);
        ImSet<PartitionToken> partitions = sourcePartitions.mapRev(castParams);

        PartitionCalc result = getPartitionCalc(syntax, type, typeEnv, mCalcTokens, exprs, orders, partitions);

        ImMap<PartitionToken, String> tokens = mTokens.immutableCopy();
        for(PartitionCalc token: mCalcTokens.immutable()) {
            mTokens.exclAdd(token, token.getSource(tokens, syntax));
        }

        return result;
    }

    private static class DistrCumPartitionType extends PartitionType {

        private final int roundLen;

        public DistrCumPartitionType(int roundLen) {
            this.roundLen = roundLen;
        }

        @Override
        protected PartitionCalc getPartitionCalc(SQLSyntax syntax, Type type, TypeEnvironment typeEnv, MExclSet<PartitionCalc> mCalcTokens, ImList<PartitionToken> exprs, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            // 1-й пробег высчитываем огругленную и часть, и первую запись на которую ра
            PartitionCalc.Aggr part = new PartitionCalc.Aggr("SUM", ListFact.singleton(exprs.get(0)), partitions);
            String proportion = "CASE WHEN prm1 IS NOT NULL AND prm2 IS NOT NULL AND prm3 IS NOT NULL AND " + syntax.getNotZero("prm3", type, typeEnv) + " IS NULL THEN 0 ELSE prm1*prm2/prm3 END";
            PartitionCalc round = new PartitionCalc("ROUND(CAST((" + proportion + ") AS numeric)," + roundLen + ")", part, exprs.get(0), exprs.get(1)); // тут местами перепутаны параметры получаются, см. конструктор
            PartitionCalc number = new PartitionCalc(new PartitionCalc.Aggr("ROW_NUMBER", orders, partitions));
            mCalcTokens.exclAdd(number);
            // 2-й пробег - результат
            PartitionCalc.Aggr totRound = new PartitionCalc.Aggr("SUM", ListFact.<PartitionToken>singleton(round), partitions);
            return new PartitionCalc("prm1 + (CASE WHEN prm2=1 THEN (prm3-prm4) ELSE 0 END)", totRound, round, number, exprs.get(1));
        }
    }

    private static class DistrRestrPartitionType extends PartitionType {

        private final boolean over;

        public DistrRestrPartitionType(boolean over) {
            this.over = over;
        }

        @Override
        protected PartitionCalc getPartitionCalc(SQLSyntax syntax, Type type, TypeEnvironment typeEnv, MExclSet<PartitionCalc> mCalcTokens, ImList<PartitionToken> exprs, ImOrderMap<PartitionToken, CompileOrder> orders, ImSet<PartitionToken> partitions) {
            String prm1 = "prm1";
            String prm2 = "prm1+prm2-prm3";
            if(syntax.noMaxImplicitCast()) {
                String rType = type.getDB(syntax, typeEnv);
                prm1 = "CAST(" + prm1 + " AS " + rType + ")";
                prm2 = "CAST(" + prm2 + " AS " + rType + ")";
            }
            String posDistrMin = syntax.getAndExpr("prm1 > 0 AND prm2>(prm3-prm1)", syntax.getMaxMin(false, prm1, prm2, type, typeEnv), type, typeEnv);
            PartitionCalc posLimit = new PartitionCalc("(" + syntax.getAndExpr("prm1 > 0", "prm1", type, typeEnv) + ")", new PartitionToken[]{exprs.get(0)});
            PartitionCalc.Aggr sumRestr = new PartitionCalc.Aggr("SUM", ListFact.singleton((PartitionToken)posLimit), orders, partitions);
            mCalcTokens.exclAdd(posLimit);

            if(!over)
                return new PartitionCalc("(" + posDistrMin + ")", sumRestr, exprs.get(0), exprs.get(1)); // тут местами перепутаны параметры получаются, см. конструктор
            else {
                PartitionCalc.Aggr sumTot = new PartitionCalc.Aggr("SUM", ListFact.<PartitionToken>singleton(posLimit), partitions);
                PartitionCalc.Aggr restrNumber = new PartitionCalc.Aggr("ROW_NUMBER", CompileOrder.reverseOrder(orders), partitions);
                String over = "CASE WHEN prm5=1 THEN (CASE WHEN prm4 IS NULL OR prm2 < 0 THEN prm2 ELSE (CASE WHEN prm2 > prm4 THEN prm2-prm4 ELSE 0 END) END) ELSE 0 END"; // первый ряд, если нет positive или prm2 - negative, тогда догоняем до prm2, иначе, если prm2 > общей суммы надо дорасписать разницу, иначе все уже расписано
                return new PartitionCalc("(" + syntax.getNotZero("(" + syntax.isNULL(posDistrMin + ",0", true) + "+" + over + ")", type, typeEnv) + ")", new PartitionToken[]{exprs.get(0), exprs.get(1)}, sumRestr, sumTot, restrNumber);
            }
        }
        public boolean isSelect() {
            return true;
        }
        public boolean canBeNull() {
            return true;
        }

        public final static PartitionType DISTR_RESTRICT = new DistrRestrPartitionType(false);
        public final static PartitionType DISTR_RESTRICT_OVER = new DistrRestrPartitionType(true);
    }
}
