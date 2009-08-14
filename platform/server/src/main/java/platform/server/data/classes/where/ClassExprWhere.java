package platform.server.data.classes.where;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.*;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;

public class ClassExprWhere extends AbstractClassWhere<VariableClassExpr, ClassExprWhere> {

    public Type getType(KeyExpr keyExpr) {
        assert wheres.length>0;
        Type type = wheres[0].get(keyExpr).getType();
        assert checkType(keyExpr,type);
        return type;
    }

    public boolean checkType(KeyExpr keyExpr,Type type) {
        for(int i=1;i<wheres.length;i++)
            assert type.isCompatible(wheres[0].get(keyExpr).getType());
        return true;
    }

    public boolean means(Where where) {
        return new MeanClassWhere(this).means(where);
    }

    private ClassExprWhere(boolean isTrue) {
        super(isTrue);
    }

    public ClassExprWhere(And<VariableClassExpr> where) {
        super(where);
    }

    public static ClassExprWhere TRUE = new ClassExprWhere(true);
    public static ClassExprWhere FALSE = new ClassExprWhere(false);

    public ClassExprWhere(VariableClassExpr key, AndClassSet classes) {
        super(key,classes);
    }


    private ClassExprWhere(And<VariableClassExpr>[] iWheres) {
        super(iWheres);
    }
    protected ClassExprWhere createThis(And<VariableClassExpr>[] iWheres) {
        return new ClassExprWhere(iWheres);
    }

    public ClassExprWhere andEquals(VariableClassExpr expr, VariableClassExpr to) {
        And<VariableClassExpr>[] rawAndWheres = newArray(wheres.length); int num=0;
        for(And<VariableClassExpr> where : wheres) {
            And<VariableClassExpr> andWhere = new And<VariableClassExpr>(where);
            if (andWhere.add(to, where.get(expr)))
                rawAndWheres[num++] = andWhere;
        }
        And<VariableClassExpr>[] andWheres = newArray(num); System.arraycopy(rawAndWheres,0,andWheres,0,num);
        return new ClassExprWhere(andWheres);
    }

    private ClassExprWhere(ClassExprWhere classes, Map<VariableClassExpr, VariableClassExpr> map) {
        super(classes, map);
    }
    public ClassExprWhere translate(KeyTranslator translator) {
        return new ClassExprWhere(this, translator.translateVariable(BaseUtils.toMap(keySet())));
    }

    public <K> ClassWhere<K> get(Map<K,AndExpr> map) {
        ClassWhere<K> transWhere = ClassWhere.STATIC(false);
        for(And<VariableClassExpr> andWhere : wheres) {
            boolean isFalse = false;
            And<K> andTrans = new And<K>();
            for(Map.Entry<K,AndExpr> mapEntry : map.entrySet()) {
                if(!andTrans.add(mapEntry.getKey(), mapEntry.getValue() instanceof StaticClassExpr ?
                        ((StaticClassExpr) mapEntry.getValue()).getStaticClass():
                        andWhere.get((VariableClassExpr) mapEntry.getValue()))) {
                    isFalse = true;
                    break;
                }
            }
            if(!isFalse)
                transWhere = transWhere.or(new ClassWhere<K>(andTrans));
        }
        return transWhere;
    }

    public DataWhereSet getFollows() {
        DataWhereSet[] follows = new DataWhereSet[wheres.length] ; int num = 0;
        for(And<VariableClassExpr> where : wheres) {
            DataWhereSet result = new DataWhereSet();
            for(int i=0;i<where.size;i++)
                result.addAll(where.getKey(i).getFollows());
            follows[num++] = result;
        }
        return new DataWhereSet(follows);
    }

    public ClassExprWhere map(Map<AndExpr,AndExpr> map) {
//        return get(BaseUtils.reverse(map)).map(BaseUtils.toMap(map.values()));
        return mapBack(BaseUtils.reverse(map));
    }

    // здесь не обязательно есть все AndExpr'ы, но здесь как map - полностью reversed
    public ClassExprWhere mapBack(Map<AndExpr,AndExpr> map) {
        ClassExprWhere transWhere = ClassExprWhere.FALSE;
        for(And<VariableClassExpr> andWhere : wheres) {
            boolean isFalse = false;
            And<VariableClassExpr> andTrans = new And<VariableClassExpr>();
            for(Map.Entry<AndExpr,AndExpr> mapEntry : map.entrySet()) {
                AndClassSet mapSet;
                if(mapEntry.getValue() instanceof StaticClassExpr)
                    mapSet = ((StaticClassExpr) mapEntry.getValue()).getStaticClass();
                else
                    if((mapSet=andWhere.getPartial((VariableClassExpr) mapEntry.getValue()))==null)
                        continue;
                if(mapEntry.getKey() instanceof StaticClassExpr) {
                    if(!((StaticClassExpr)mapEntry.getKey()).getStaticClass().inSet(mapSet)) {
                        isFalse = true;
                        break;
                    }
                } else {
                    boolean add = andTrans.add((VariableClassExpr) mapEntry.getKey(),mapSet);
                    assert add;
                }
            }
            if(!isFalse)
                transWhere = transWhere.or(new ClassExprWhere(andTrans));
        }
        return transWhere;
    }
}
