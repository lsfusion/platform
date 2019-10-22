package lsfusion.server.data.expr.formula;

import lsfusion.base.col.WrapMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.conversion.CompatibleTypeConversion;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.data.integral.IntegralClass;

public class LinearOperandMap extends WrapMap<Expr,Integer> {

    public LinearOperandMap(ImMap<? extends Expr, ? extends Integer> map) {
        super(map);
    }

    public IntegralClass getType(KeyType keyType) {
        return (IntegralClass) AbstractFormulaImpl.getCompatibleType(ListExprType.create(keyType, keys().toList()), CompatibleTypeConversion.instance);
    }

    public int hashOuter(HashContext hashContext) {
        int result = 0;
        for(int i=0,size=size();i<size;i++)
            result += (getValue(i)-1)*31 + getKey(i).hashOuter(hashContext);
        return result;
    }

    public String getSource(CompileSource compile) {

//        if(size()==1) {
//            Map.Entry<Expr,Integer> operand = BaseUtils.singleEntry(this);
//            return "(" + addToString(true, operand.getKey().getSource(compile), operand.getValue()) + ")";
//        }

        String source = "";
//        Where linearWhere = Where.FALSE();
//        Collection<String> orderWhere = new ArrayList<String>();
        for(int i=0,size=size();i<size;i++) {
            assert getValue(i)!=0;
            source = source + addToString(source.length() == 0, compile.syntax.isNULL(getKey(i).getSource(compile)+",0", true), getValue(i)); // getSource needValue=true так как a (+) b NULL не NULL зависит от значений
/*            if(PartitionExpr.isWhereCalculated(operand.getKey())) // ?? зачем
                orderWhere.add(operand.getKey().getSource(compile)+" IS NOT NULL");
            else
                linearWhere = linearWhere.or(operand.getKey().getWhere());*/
        }
        return compile.syntax.getNotZero(source, getType(compile.keyType), compile.env);//"(CASE WHEN " + linearWhere.getSource(compile) + (orderWhere.size()==0?"":" OR "+BaseUtils.toString(orderWhere," OR ")) + " THEN " + (source.length()==0?"0":source) + " ELSE " + SQLSyntax.NULL + " END)";
    }

    public String toString() {
        String result = "";
        for(int i=0,size=size();i<size;i++)
            result = result + addToString(result.length() == 0, getKey(i).toString(), getValue(i));
        return "L(" + result + ")";
    }

    protected static String addToString(boolean first, String string, int coeff) {
        assert coeff!=0;
        if(coeff>0) return (first?"":"+") + (coeff==1?"":coeff+"*") + string;
        if(coeff==-1) return "-"+string;
        return coeff+"*"+string; // <0
    }

    public Expr packFollowFalse(Where where) {
        MLinearOperandMap followedMap = new MLinearOperandMap();
        for(int i=0,size=size();i<size;i++) {
            assert !getValue(i).equals(0);
            followedMap.add(getKey(i).followFalse(where, true),getValue(i));
        }
        return followedMap.getExpr();
    }

    protected LinearOperandMap translateOuter(MapTranslate translator) {
        return new LinearOperandMap(translator.translateExprKeys(map));
    }
}
