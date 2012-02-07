package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.base.BaseUtils;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.SubQueryExpr;
import platform.server.data.query.IQuery;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyQueryType;
import platform.server.session.PropertyChanges;

import java.util.Map;

@Aspect
public class WrapComplexityAspect {

    <K extends PropertyInterface> Expr wrapComplexity(Expr expr, Where where, Property<K> property, Map<K, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        Expr wrapExpr = expr;
        if(expr.getComplexity(true) > Settings.instance.getLimitWrapComplexity()) {
            System.out.println("WRAP COMPLEX EXPR " + property + " : " + expr.getComplexity(true));
            wrapExpr = SubQueryExpr.create(expr);
        }
        if(where != null) {
            if(where.getComplexity(true) > Settings.instance.getLimitWrapComplexity()) {
                System.out.println("WRAP COMPLEX WHERE " + property + " : " + where.getComplexity(true));
                where = SubQueryExpr.create(where.and(expr.getWhere().or(property.getExpr(joinImplement).getWhere())));
            }
            changedWhere.add(where);
        }
        return wrapExpr;
    }

    public <T extends PropertyInterface> Expr getJoinExpr(ProceedingJoinPoint thisJoinPoint, Property<T> property, Map<T, ? extends Expr> joinExprs, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        if(!property.isFull()) // если ключей не хватает wrapp'ить нельзя
            return (Expr) thisJoinPoint.proceed();
        WhereBuilder cascadeWhere = Property.cascadeWhere(changedWhere);
        return wrapComplexity((Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, propChanges, cascadeWhere}),
                changedWhere!=null?cascadeWhere.toWhere():null, property, joinExprs, changedWhere);
    }
    @Around("execution(* platform.server.logics.property.Property.getJoinExpr(java.util.Map,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        return getJoinExpr(thisJoinPoint, property, joinExprs, propChanges, changedWhere);
    }

    public <T extends PropertyInterface> IQuery<T, String> getQuery(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges, PropertyQueryType queryType, Map<T, ? extends Expr> interfaceValues) throws Throwable {
        assert property.isFull();
        IQuery<T, String> query = (IQuery<T, String>) thisJoinPoint.proceed();

        Map<T, KeyExpr> mapKeys = query.getMapKeys();
        Expr expr = query.getExpr("value");

        boolean changedWhere = queryType.needChange();
        Where where = changedWhere ? query.getExpr("changed").getWhere() : null;
        WhereBuilder wrapWhere = changedWhere ? new WhereBuilder() : null;
        Expr wrapExpr = wrapComplexity(expr, where, property, BaseUtils.merge(interfaceValues, mapKeys), wrapWhere);

        if(BaseUtils.hashEquals(expr, wrapExpr) && BaseUtils.nullHashEquals(where, changedWhere ? wrapWhere.toWhere() : null))
            return query;
        else {
            Query<T, String> wrappedQuery = new Query<T, String>(mapKeys);
            wrappedQuery.properties.put("value", wrapExpr);
            if(changedWhere)
                wrappedQuery.properties.put("changed", ValueExpr.get(wrapWhere.toWhere()));
            return wrappedQuery;
        }
    }
    @Around("execution(* platform.server.logics.property.Property.getQuery(platform.server.session.PropertyChanges,platform.server.logics.property.PropertyQueryType,java.util.Map)) && target(property) && args(propChanges, queryType, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges, PropertyQueryType queryType, Map interfaceValues) throws Throwable {
        return getQuery(thisJoinPoint, property, propChanges, queryType, interfaceValues);
    }
}
