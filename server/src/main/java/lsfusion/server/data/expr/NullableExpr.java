package lsfusion.server.data.expr;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.data.expr.where.NotNullWhere;
import lsfusion.server.data.query.innerjoins.UpWhere;
import lsfusion.server.data.query.stat.UnionJoin;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;

public abstract class NullableExpr extends VariableSingleClassExpr implements NullableExprInterface {

    @Override
    public Where calculateOrWhere() {
        return Where.TRUE;
    }

    @Override
    public Where calculateNotNullWhere() { // assert result instanceof NotNull || result.isTrue()
        return Where.TRUE;
    }

    public abstract class NotNull extends NotNullWhere {
        protected BaseExpr getExpr() {
            return NullableExpr.this;
        }
    }

    // особенность в том что логика следствий выражений, используется для булевой логики и для логики выполнения (определения Inner Join) 
    // соответственно в одной "пустые" join'ы нужны, в другой нет
    public final static boolean FOLLOW = false; // в булевой логике
    public final static boolean INNERJOINS = true; // одновременно и при конструировании InnerJoins и проверке contains и при проталкивании InnerJoins (,впоследствии можно разделить)

    // второй параметр, предполагается true при использовании в логике InnerJoins (при выяснении isInner в компиляции в CompiledQuery в основном), false - в булевой логике - логике следствий
    // !!! есть упрощенная копия метода в NullableKeyExpr из-за отсутствия множественного наследования и для скорости (так как есть кэш)
    private ImSet<NullableExprInterface> exprThisFollows = null;
    @ManualLazy
    public ImSet<NullableExprInterface> getExprFollows(boolean includeThis, boolean includeInnerWithoutNotNull, boolean recursive) {
        assert includeThis || recursive;
        if(recursive) {
            if(includeThis && (includeInnerWithoutNotNull || hasNotNull())) {
                if(!includeInnerWithoutNotNull || !hasExprFollowsWithoutNotNull()) {
                    if(exprThisFollows==null)
                        exprThisFollows = SetFact.addExcl(getExprFollows(includeInnerWithoutNotNull, true), this);
                    return exprThisFollows;
                }
                return SetFact.addExcl(getExprFollows(includeInnerWithoutNotNull, true), this);
            }            
            return getExprFollows(includeInnerWithoutNotNull, true);
        } 
        
        // не кэшируем так как редко используется
        if(includeInnerWithoutNotNull || hasNotNull())
            return SetFact.<NullableExprInterface>singleton(this);
        else
            return SetFact.EMPTY();
    }

    @Override
    public boolean hasExprFollowsWithoutNotNull() {
        if(!hasNotNull())
            return true;
        return super.hasExprFollowsWithoutNotNull();
    }

    // множественное наследование
    public static void fillFollowSet(NullableExprInterface notNull, MSet<DataWhere> fillSet) {
        assert notNull.hasNotNull();
        fillSet.add((DataWhere)notNull.getNotNullWhere());
    }
    
    public void fillFollowSet(MSet<DataWhere> fillSet) {
        fillFollowSet(this, fillSet);
    }

    public boolean hasNotNull() {
        return hasNotNull(this);
    }

    public static boolean hasNotNull(NullableExprInterface notNullExpr) {
        return notNullExpr.getNotNullWhere() instanceof DataWhere;
    }

    public static boolean hasExprFollowsWithoutNotNull(ImCol<BaseExpr> exprs) {
        for(int i=0,size=exprs.size();i<size;i++)
            if(exprs.get(i).hasExprFollowsWithoutNotNull())
                return true;
        return false;
    }

    public static ImSet<DataWhere> getFollows(ImSet<NullableExprInterface> exprFollows) {
        MSet<DataWhere> result = SetFact.mSet();
        for(int i=0,size=exprFollows.size();i<size;i++)
            exprFollows.get(i).fillFollowSet(result);
        return result.immutable();
    }

    public static ImSet<InnerExpr> getInnerExprs(ImSet<NullableExprInterface> set, Result<ImSet<UnionJoin>> unionJoins) {
        boolean hasNotInner = false;
        for(int i=0,size=set.size();i<size;i++) // оптимизация
            if(!(set.get(i) instanceof InnerExpr)) {
                hasNotInner = true;
                break;
            }
        if(!hasNotInner) {
            if(unionJoins!=null)
                unionJoins.set(SetFact.<UnionJoin>EMPTY());
            return BaseUtils.immutableCast(set);
        }

        MSet<UnionJoin> mUnionJoins = null;
        if(unionJoins!=null)
            mUnionJoins = SetFact.mSetMax(set.size());

        MSet<InnerExpr> mResult = SetFact.mSet();
        for(int i=0,size=set.size();i<size;i++) {
            NullableExprInterface expr = set.get(i);
            if(expr instanceof InnerExpr)
                mResult.add((InnerExpr)expr);
            else {
                if(mUnionJoins!=null && expr instanceof UnionExpr)
                    mUnionJoins.add(((UnionExpr)expr).getBaseJoin());
                mResult.addAll(getInnerExprs(expr.getExprFollows(NullableExpr.INNERJOINS, false), unionJoins));
            }
        }

        if(unionJoins!=null)
            unionJoins.set(mUnionJoins.immutable());
        return mResult.immutable();
    }

}
