package lsfusion.server.caches;

import lsfusion.server.logics.property.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.Settings;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.SubQueryExpr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

@Aspect
public class WrapComplexityAspect {

    <K extends PropertyInterface> Expr wrapComplexity(Expr expr, Where where, CalcProperty<K> property, ImMap<K, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        Expr wrapExpr = expr;
        if(expr.getComplexity(true) > Settings.get().getLimitWrapComplexity()) {
//            System.out.println("WRAP COMPLEX EXPR " + property + "(" + property.getSID() + ") : " + expr.getComplexity(true));
            wrapExpr = SubQueryExpr.create(expr);
        }
        if(where != null) {
            if(where.getComplexity(true) > Settings.get().getLimitWrapComplexity()) {
//                System.out.println("WRAP COMPLEX WHERE " + property + " : " + where.getComplexity(true));
                where = SubQueryExpr.create(where.and(expr.getWhere().or(property.getExpr(joinImplement).getWhere())));
            }
            changedWhere.add(where);
        }
        return wrapExpr;
    }

    public <T extends PropertyInterface> Expr getJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty<T> property, ImMap<T, ? extends Expr> joinExprs, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        if((Settings.get().isDisableWrapComplexity() && !property.complex && !(property instanceof OldProperty && Settings.get().isEnablePrevWrapComplexity())) || !property.isFull(calcType.getAlgInfo())) // если ключей не хватает wrapp'ить нельзя
            return (Expr) thisJoinPoint.proceed();
        WhereBuilder cascadeWhere = CalcProperty.cascadeWhere(changedWhere);
        return wrapComplexity((Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, calcType, propChanges, cascadeWhere}),
                changedWhere!=null?cascadeWhere.toWhere():null, property, joinExprs, changedWhere);
    }
    @Around("execution(* lsfusion.server.logics.property.CalcProperty.getJoinExpr(lsfusion.base.col.interfaces.immutable.ImMap,lsfusion.server.logics.property.CalcType,lsfusion.server.session.PropertyChanges,lsfusion.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,calcType,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, CalcProperty property, ImMap joinExprs, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        return getJoinExpr(thisJoinPoint, property, joinExprs, calcType, propChanges, changedWhere);
    }

    public <T extends PropertyInterface> IQuery<T, String> getQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) throws Throwable {
        assert property.isNotNull(calcType.getAlgInfo());
        IQuery<T, String> query = (IQuery<T, String>) thisJoinPoint.proceed();
        
        if((Settings.get().isDisableWrapComplexity() && !property.complex && !(property instanceof OldProperty && Settings.get().isEnablePrevWrapComplexity())) || !property.isFull(calcType.getAlgInfo()))
            return query;

        ImRevMap<T, KeyExpr> mapKeys = query.getMapKeys();
        Expr expr = query.getExpr("value");

        boolean changedWhere = queryType.needChange();
        Where where = changedWhere ? query.getExpr("changed").getWhere() : null;
        WhereBuilder wrapWhere = changedWhere ? new WhereBuilder() : null;
        Expr wrapExpr = wrapComplexity(expr, where, property, MapFact.addExcl(interfaceValues, mapKeys), wrapWhere);

        if(BaseUtils.hashEquals(expr, wrapExpr) && BaseUtils.nullHashEquals(where, changedWhere ? wrapWhere.toWhere() : null))
            return query;
        else {
            QueryBuilder<T, String> wrappedQuery = new QueryBuilder<T, String>(mapKeys);
            wrappedQuery.addProperty("value", wrapExpr);
            if(changedWhere)
                wrappedQuery.addProperty("changed", ValueExpr.get(wrapWhere.toWhere()));
            return wrappedQuery.getQuery();
        }
    }
    @Around("execution(* lsfusion.server.logics.property.CalcProperty.getQuery(lsfusion.server.logics.property.CalcType,lsfusion.server.session.PropertyChanges,lsfusion.server.logics.property.PropertyQueryType,lsfusion.base.col.interfaces.immutable.ImMap)) " +
            "&& target(property) && args(calcType, propChanges, queryType, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, CalcProperty property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap interfaceValues) throws Throwable {
        return getQuery(thisJoinPoint, property, calcType, propChanges, queryType, interfaceValues);
    }
}
