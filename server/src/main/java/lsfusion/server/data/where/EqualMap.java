package lsfusion.server.data.where;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.server.data.expr.BaseExpr;

public class EqualMap extends HMap<BaseExpr,Equal> {

    public Equal[] comps;
    public int num = 0;

    public EqualMap(int max) {
        super(MapFact.<BaseExpr, Equal>override());
        comps = new Equal[max];
    }

    Equal getEqual(BaseExpr expr) {
        Equal equal = get(expr);
        if(equal==null) {
            equal = new Equal(expr, comps.length);
            add(expr,equal);
            comps[num++] = equal;
        }
        return equal;
    }

    public boolean add(BaseExpr expr1, BaseExpr expr2) {
        Equal equal1 = getEqual(expr1);
        Equal equal2 = getEqual(expr2);

        if(equal1.equals(equal2))
            return true;

        // с этими проверками нужно быть аккуратнее, так как в MeanClassWhere используется просто для собирания "компонент"
        if(equal1.value==null) {
            equal1.value = equal2.value;
        } else
            if(equal2.value!=null && !equal1.value.equals(equal2.value)) // если равенство разных value, то false
                return false;
        if(equal1.staticValue==null) {
            equal1.staticValue = equal2.staticValue;
        } else
        if(equal2.staticValue!=null && !equal1.staticValue.equals(equal2.staticValue)) // если равенство разных value, то false
            return false;

        for(int i=0;i<equal2.size;i++) // "перекидываем" все компоненты в первую
            add(equal2.exprs[i],equal1);
        System.arraycopy(equal2.exprs,0,equal1.exprs,equal1.size,equal2.size);
        equal1.size += equal2.size;
        equal2.dropped = true;

        return true;
    }
}
