package platform.server.data.query.exprs;

import platform.server.data.query.*;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.*;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать AndExpr
public class LinearExpr extends AndExpr {

    class Operand {
        SourceExpr expr;
        int coeff;

        Operand(SourceExpr iExpr, int iCoeff) {
            expr = iExpr;
            coeff = iCoeff;
        }

        String addToString(boolean first, String string) {
            if(coeff==0) return (first?"0":"");
            if(coeff>0) return (first?"":"+") + (coeff==1?"":coeff+"*") + string;
            if(coeff==-1) return "-"+string;
            return coeff+"*"+string; // <0
        }
    }

    // !!!! он меняется при add'е, но конструктора пока нету так что все равно
    Collection<Operand> operands = new ArrayList<Operand>();
    void add(SourceExpr expr,Integer coeff) {
        for(Operand operand : operands)
            if(operand.expr.hashCode()==expr.hashCode() && operand.expr.equals(expr)) {
                operand.coeff += coeff;
                return;
            }
        operands.add(new Operand(expr, coeff));
    }

    LinearExpr() {
    }
    public LinearExpr(SourceExpr expr,SourceExpr opExpr,boolean sum) {
        add(expr,1);
        add(opExpr,(sum?1:-1));
    }
    public LinearExpr(SourceExpr expr,Integer coeff) {
        add(expr,coeff);
    }

    // при translate'ы вырезает LinearExpr'ы

    public Type getType() {
        Type type = operands.iterator().next().expr.getType();
        if(type==Type.bit) // биты переведем в integer'ы если идут математические операции
            return Type.integer;
        else
            return type;
    }

    // возвращает Where на notNull
    protected Where calculateWhere() {
        Where result = Where.FALSE;
        for(Operand operand : operands)
            result = result.or(operand.expr.getWhere());
        return result;
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {

        if(operands.size()==1) {
            Operand operand = operands.iterator().next();
            return operand.addToString(true,operand.expr.getSource(queryData, syntax));
        }

        String source = "";
        String notNull = "";
        for(Operand operand : operands) {
            notNull = (notNull.length()==0?"":notNull+" OR ")+operand.expr.getWhere().getSource(queryData, syntax);
            source = source + operand.addToString(source.length()==0,
                    syntax.isNULL(operand.expr.getSource(queryData, syntax),operand.expr.getType().getEmptyString(),true));
        }
        return "(CASE WHEN " + notNull + " THEN " + source + " ELSE NULL END)";
    }

    public String toString() {
        String result = "";
        for(Operand operand : operands)
            result = result + operand.addToString(result.length()==0,operand.expr.toString());
        return "L(" + result + ")";
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        for(Operand operand : operands)
            operand.expr.fillJoins(joins, values);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        // просто гоним по операндам
        for(Operand operand : operands)
            operand.expr.fillJoinWheres(joins, andWhere);
    }

    // для кэша
    public boolean equals(SourceExpr expr, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        if(!(expr instanceof LinearExpr)) return false;

        LinearExpr linearExpr = (LinearExpr) expr;
        if(operands.size()!=linearExpr.operands.size()) return false;

        for(Operand exprOperand : linearExpr.operands) {
            boolean found = false;
            for(Operand operand : operands)
                if(operand.coeff==exprOperand.coeff && operand.expr.hash() == exprOperand.expr.hash() &&
                        operand.expr.equals(exprOperand.expr, mapValues, mapKeys, mapJoins)) {
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    protected int getHash() {
        int result = 0;
        for(Operand operand : operands)
            result += (operand.coeff)*31 + operand.expr.hash();
        return result;
    }


    public boolean equals(Object obj) {
        if(!(obj instanceof LinearExpr)) return false;

        LinearExpr linearExpr = (LinearExpr) obj;
        if(operands.size()!=linearExpr.operands.size()) return false;

        for(Operand exprOperand : linearExpr.operands) {
            boolean found = false;
            for(Operand operand : operands)
                if(operand.coeff==exprOperand.coeff && operand.expr.hashCode() == exprOperand.expr.hashCode() &&
                        operand.expr.equals(exprOperand.expr)) {
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    SourceExpr translateCase(Map<Operand,? extends SourceExpr> mapOperands) {
        LinearExpr transLinear = new LinearExpr();

        for(Map.Entry<Operand,? extends SourceExpr> mapOperand : mapOperands.entrySet()) {
            SourceExpr transExpr = mapOperand.getValue();
            if(transExpr instanceof LinearExpr) { // LinearExpr, его нужно влить сюда
                for(Operand transOperand : ((LinearExpr)transExpr).operands)
                    transLinear.add(transOperand.expr,mapOperand.getKey().coeff*transOperand.coeff);
            } else {
            if(transExpr instanceof ValueExpr && mapOperand.getKey().coeff!=1)
                transLinear.add(new ValueExpr(((ValueExpr)transExpr).object.multiply(mapOperand.getKey().coeff),getType()),1);
            else
            if(!(transExpr instanceof NullExpr))
                transLinear.add(transExpr,mapOperand.getKey().coeff);
            }
        }

        if(transLinear.operands.size()==0)
            return getType().getExpr(null);

        if(transLinear.operands.size()==1) {
            Operand operand = transLinear.operands.iterator().next();
            if(operand.coeff==1) return operand.expr;
        }

        return transLinear;
    }

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    public SourceExpr translate(Translator translator) {

        Map<Operand, SourceExpr> mapOperands = new HashMap<Operand, SourceExpr>();
        for(Operand operand : operands)
            mapOperands.put(operand,operand.expr.translate(translator));
        return translateCase(mapOperands);

/*        Map<Operand,SourceExpr> mapOperands = new HashMap<Operand, SourceExpr>();
        for(Operand operand : operands)
            mapOperands.put(operand,operand.expr);

        ExprCaseList result = new ExprCaseList();
        for(MapCase<Operand> mapCase : CaseExpr.translateCase(mapOperands, translator, true, true))  // здесь напрямую потому как MapCaseList уже все проверил
            result.add(new ExprCase(mapCase.where,translateCase(mapCase.data))); // кстати могут быть и одинаковые case'ы
        return result.getExpr(getType());*/
    }

    protected int getHashCode() {
        int result = 0;
        for(Operand operand : operands)
            result += (operand.coeff)*31 + operand.expr.hashCode();
        return result;
    }

    public DataWhereSet getFollows() {
        SourceExpr operand;
        if(operands.size()==1 && ((operand = operands.iterator().next().expr) instanceof AndExpr))
            return ((AndExpr)operand).getFollows();
        else
            return new DataWhereSet();
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }

    public SourceExpr followFalse(Where where) {
        LinearExpr followedLinear = new LinearExpr();
        boolean changed = false;
        for(Operand operand : operands) {
            SourceExpr followedOperand = operand.expr.followFalse(where);
            followedLinear.add(followedOperand,operand.coeff);
            changed = changed || (followedOperand != operand.expr);
        }
        if(!changed)
            return this;
        else
            return followedLinear;
    }
}