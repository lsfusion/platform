package lsfusion.server.physics.exec.hint;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.SubQueryExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyQueryType;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class WrapComplexityAspect {

    <K extends PropertyInterface> Expr wrapComplexity(Expr expr, Where where, Property<K> property, ImMap<K, ? extends Expr> joinImplement, WhereBuilder changedWhere, CalcType calcType, PropertyChanges propChanges) {
        Expr wrapExpr = expr;
        if(expr.getComplexity(true) > Settings.get().getLimitWrapComplexity()) {
//            System.out.println("WRAP COMPLEX EXPR " + property + "(" + property.getSID() + ") : " + expr.getComplexity(true));
            wrapExpr = SubQueryExpr.create(expr, calcType instanceof CalcClassType);
        }
        if(where != null) {
            if(where.getComplexity(true) > Settings.get().getLimitWrapComplexity()) {
//                System.out.println("WRAP COMPLEX WHERE " + property + " : " + where.getComplexity(true));
                where = SubQueryExpr.create(where.and(expr.getWhere().or(property.getPrevExpr(joinImplement, calcType, propChanges).getWhere())), calcType instanceof CalcClassType);
            }
            changedWhere.add(where);
        }
        return wrapExpr;
    }

    public <T extends PropertyInterface> Expr getJoinExpr(ProceedingJoinPoint thisJoinPoint, Property<T> property, ImMap<T, ? extends Expr> joinExprs, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        if((Settings.get().isDisableWrapComplexity() && !property.isComplex() && !(property instanceof OldProperty && Settings.get().isEnablePrevWrapComplexity()))) // || !property.isFull(calcType.getAlgInfo())) // it seems there is no need to check for property "fullness" at least for explicit COMPLEX properties
            return (Expr) thisJoinPoint.proceed();
        WhereBuilder cascadeWhere = Property.cascadeWhere(changedWhere);
        return wrapComplexity((Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, calcType, propChanges, cascadeWhere}),
                changedWhere!=null?cascadeWhere.toWhere():null, property, joinExprs, changedWhere, calcType, propChanges);
    }
    @Around("execution(* lsfusion.server.logics.property.Property.getJoinExpr(lsfusion.base.col.interfaces.immutable.ImMap,lsfusion.server.logics.property.CalcType,lsfusion.server.logics.action.session.change.PropertyChanges,lsfusion.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,calcType,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, Property property, ImMap joinExprs, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        return getJoinExpr(thisJoinPoint, property, joinExprs, calcType, propChanges, changedWhere);
    }

    public <T extends PropertyInterface> IQuery<T, String> getQuery(ProceedingJoinPoint thisJoinPoint, Property property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap<T, ? extends Expr> interfaceValues) throws Throwable {
        assert property.isNotNull(calcType.getAlgInfo());
        IQuery<T, String> query = (IQuery<T, String>) thisJoinPoint.proceed();
        
        if((Settings.get().isDisableWrapComplexity() && !property.isComplex() && !(property instanceof OldProperty && Settings.get().isEnablePrevWrapComplexity()))) // || !property.isFull(calcType.getAlgInfo()))  // it seems there is no need to check for property "fullness" at least for explicit COMPLEX properties
            return query;

        ImRevMap<T, KeyExpr> mapKeys = query.getMapKeys();
        Expr expr = query.getExpr("value");

        boolean changedWhere = queryType.needChange();
        Where where = changedWhere ? query.getExpr("changed").getWhere() : null;
        WhereBuilder wrapWhere = changedWhere ? new WhereBuilder() : null;
        Expr wrapExpr = wrapComplexity(expr, where, property, MapFact.addExcl(interfaceValues, mapKeys), wrapWhere, calcType, propChanges);
        
        if(BaseUtils.hashEquals(expr, wrapExpr) && BaseUtils.nullHashEquals(where, changedWhere ? wrapWhere.toWhere() : null))
            return query;
        else {
            QueryBuilder<T, String> wrappedQuery = new QueryBuilder<>(mapKeys);
            wrappedQuery.addProperty("value", wrapExpr);
            if(changedWhere)
                wrappedQuery.addProperty("changed", ValueExpr.get(wrapWhere.toWhere()));
            return wrappedQuery.getQuery();
        }
    }
    @Around("execution(* lsfusion.server.logics.property.Property.getQuery(lsfusion.server.logics.property.CalcType,lsfusion.server.logics.action.session.change.PropertyChanges,lsfusion.server.logics.property.PropertyQueryType,lsfusion.base.col.interfaces.immutable.ImMap)) " +
            "&& target(property) && args(calcType, propChanges, queryType, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, Property property, CalcType calcType, PropertyChanges propChanges, PropertyQueryType queryType, ImMap interfaceValues) throws Throwable {
        return getQuery(thisJoinPoint, property, calcType, propChanges, queryType, interfaceValues);
    }
}
