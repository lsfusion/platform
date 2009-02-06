package platformlocal;

import java.util.*;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать AndExpr
class LinearExpr extends AndExpr {

    Map<SourceExpr,Integer> operands = new HashMap<SourceExpr, Integer>();
    void add(SourceExpr expr,Integer coeff) {
        Integer prevCoeff = operands.get(expr);
        if(prevCoeff!=null) coeff = coeff + prevCoeff;
        if(coeff.equals(0)) {
            if(prevCoeff!=null) {
                throw new RuntimeException("!!!!! Сейчас она при убирании операндов немного по другому работать будет ???? В этом тоже глюк может быть");
                //operands.remove(expr);
            }
        } else
            operands.put(expr,coeff);
    }

    LinearExpr() {
    }
    LinearExpr(SourceExpr expr,SourceExpr opExpr,boolean sum) {
        add(expr,1);
        add(opExpr,(sum?1:-1));
    }
    LinearExpr(SourceExpr expr,Integer coeff) {
        add(expr,coeff);
    }

    // при translate'ы вырезает LinearExpr'ы

    Type getType() {
        return operands.keySet().iterator().next().getType();
    }

    // возвращает Where на notNull
    Where getWhere() {
        Where result = new OrWhere();
        for(SourceExpr operand : operands.keySet())
            result = result.or(operand.getWhere());
        return result;
    }

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String source = "";
        String notNull = "";
        for(Map.Entry<SourceExpr,Integer> operand : operands.entrySet()) {
            notNull = (notNull.length()==0?"":notNull+"   OR    ")+operand.getKey().getWhere().getSource(queryData, syntax);
            source = (source.length()==0?"":source+"+") + (operand.getValue().equals(1)?"":"("+operand.getValue()+")*") +
                    syntax.isNULL(operand.getKey().getSource(queryData, syntax),operand.getKey().getType().getEmptyString(),true);
        }
        return "(CASE WHEN " + notNull + " THEN " + source + " ELSE NULL    END)";
    }

    public String toString() {
        String result = "";
        for(Map.Entry<SourceExpr,Integer> operand : operands.entrySet())
            result = (result.length()==0?"":result+"+") + operand.getValue() + "*" + operand.getKey();
        return "L(" + result + ")";
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        for(SourceExpr operand : operands.keySet())
            operand.fillJoins(joins, values);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        // просто гоним по операндам
        for(SourceExpr operand : operands.keySet())
            operand.fillJoinWheres(joins, andWhere);
    }

    // для кэша
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(!(expr instanceof LinearExpr)) return false;

        LinearExpr linearExpr = (LinearExpr) expr;
        if(operands.size()!=linearExpr.operands.size()) return false;

        for(Map.Entry<SourceExpr,Integer> exprOperand : linearExpr.operands.entrySet()) {
            boolean found = false;
            for(Map.Entry<SourceExpr,Integer> operand : operands.entrySet())
                if(operand.getValue().equals(exprOperand.getValue()) && operand.getKey().hash() == exprOperand.getKey().hash() && 
                        operand.getKey().equals(exprOperand.getKey(), mapExprs, mapWheres)) {
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    int getHash() {
        int result = 0;
        for(Map.Entry<SourceExpr,Integer> operand : operands.entrySet())
            result += operand.getValue()*31 + operand.getKey().hash();
        return result;
    }


    public boolean equals(Object obj) {
        if(!(obj instanceof LinearExpr)) return false;

        LinearExpr linearExpr = (LinearExpr) obj;
        if(operands.size()!=linearExpr.operands.size()) return false;

        for(Map.Entry<SourceExpr,Integer> exprOperand : linearExpr.operands.entrySet()) {
            boolean found = false;
            for(Map.Entry<SourceExpr,Integer> operand : operands.entrySet())
                if(operand.getValue().equals(exprOperand.getValue()) && operand.getKey().hashCode() == exprOperand.getKey().hashCode() &&
                        operand.getKey().equals(exprOperand.getKey())) {
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    SourceExpr translate(Translator Translator) {
        LinearExpr transLinear = new LinearExpr();
        for(Map.Entry<SourceExpr,Integer> operand : operands.entrySet()) {
            SourceExpr transExpr = operand.getKey().translate(Translator);
            if(transExpr instanceof LinearExpr) {
                for(Map.Entry<SourceExpr,Integer> transOperand : ((LinearExpr)transExpr).operands.entrySet())
                    transLinear.add(transOperand.getKey(),operand.getValue()*transOperand.getValue());
            } else // LinearExpr, его нужно влить сюда
                if(!(transExpr instanceof NullExpr))
                    transLinear.add(transExpr,operand.getValue());
        }

        if(transLinear.operands.size()==0)
            return new NullExpr(getType());

        if(transLinear.operands.size()==1) {
            Map.Entry<SourceExpr, Integer> operand = transLinear.operands.entrySet().iterator().next();
            return new FormulaExpr(operand.getKey(),operand.getValue()); 
        }

        if(hashCode()==transLinear.hashCode() && equals(transLinear))
            return this;
        else
            return transLinear;
    }


    public int hashCode() {
        return getHash();
    }

    boolean follow(DataWhere Where) {
        return false;
    }

    Set<DataWhere> getFollows() {
        return new HashSet<DataWhere>();
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }
}