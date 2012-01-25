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
import platform.server.data.query.ParsedQuery;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
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
        WhereBuilder cascadeWhere = Property.cascadeWhere(changedWhere);
        return wrapComplexity((Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, propChanges, cascadeWhere}),
                changedWhere!=null?cascadeWhere.toWhere():null, property, joinExprs, changedWhere);
    }
    @Around("execution(* platform.server.logics.property.Property.getJoinExpr(java.util.Map,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        return getJoinExpr(thisJoinPoint, property, joinExprs, propChanges, changedWhere);
    }

    public <T extends PropertyInterface> Query<T, String> getQuery(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges, Boolean changedWhere, Map<T, ? extends Expr> interfaceValues) throws Throwable {
        Query<T, String> query = (Query<T, String>) thisJoinPoint.proceed();
        ParsedQuery<T, String> parsedQuery = query.parse();
        
        Map<T, KeyExpr> mapKeys = parsedQuery.getMapKeys();
        Expr expr = parsedQuery.getExpr("value");

        Where where = changedWhere ? parsedQuery.getExpr("changed").getWhere() : null;
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
    @Around("execution(* platform.server.logics.property.Property.getQuery(platform.server.session.PropertyChanges,java.lang.Boolean,java.util.Map)) && target(property) && args(propChanges, changedWhere, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges, Boolean changedWhere, Map interfaceValues) throws Throwable {
        return getQuery(thisJoinPoint, property, propChanges, changedWhere, interfaceValues);
    }
}
