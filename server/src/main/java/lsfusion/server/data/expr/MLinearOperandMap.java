package lsfusion.server.data.expr;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.expr.where.cases.CaseExpr;

public class MLinearOperandMap {

    private final MMap<Expr, Integer> mMap = MapFact.mMap(MapFact.<Expr>addLinear()); // теоретически можно было бы size протянуть

    private void add(LinearOperandMap map, int coeff) {
        for(int i=0,size=map.size();i<size;i++)
            add(map.getKey(i),map.getValue(i)*coeff);
    }

    // !!!! он меняется при add'е, но конструктора пока нету так что все равно
    public void add(Expr expr,int coeff) {
        if(expr.isNull()) // если null не добавляем
            return;

        if(expr instanceof LinearExpr)
            add(((LinearExpr)expr).map,coeff);
        else
            mMap.add(expr, coeff);
    }

    public Expr getExpr() {

        ImMap<Expr,Integer> map = mMap.immutable().removeValues(0);
        if(map.size()==0)
            return CaseExpr.NULL;

        // нельзя делать эту оптимизацию так как идет проверка на 0 в логике
//        if(size()==1) {
//            Map.Entry<Expr, Integer> entry = BaseUtils.singleEntry(this);
//            if(entry.getValue().equals(1))
//                return entry.getKey();
//        }
        return new LinearExpr(new LinearOperandMap(map));
    }
}
