package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.IQuery;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.FormInstance;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyQueryType;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;

import java.util.HashMap;
import java.util.Map;

import static platform.base.BaseUtils.max;

@Aspect
public class AutoHintsAspect {

    public static ThreadLocal<FormInstance> catchAutoHint = new ThreadLocal<FormInstance>();
    
    public Object callAutoHint(ProceedingJoinPoint thisJoinPoint, Property property, Modifier modifier) throws Throwable {
        if(!Settings.instance.isDisableAutoHints() && modifier instanceof FormInstance) { // && property.hasChanges(modifier) иначе в рекурсию уходит при changeModifier'е, надо было бы внутрь перенести
            FormInstance formInstance = (FormInstance) modifier;
            catchAutoHint.set(formInstance);
            Object result;
            try {
                result = thisJoinPoint.proceed();
                assert catchAutoHint.get()==modifier;
                catchAutoHint.set(null);
            } catch (AutoHintException e) {
                assert catchAutoHint.get()==modifier;
                catchAutoHint.set(null);
                if(e.lowstat)
                    formInstance.addHintIncrement(e.property);
                else
                    formInstance.addHintNoUpdate(property);
                result = ((MethodSignature)thisJoinPoint.getSignature()).getMethod().invoke(thisJoinPoint.getTarget(), thisJoinPoint.getArgs());
            }
            return result;
        } else
            return thisJoinPoint.proceed();
    }

    @Around("execution(* platform.server.logics.property.Property.getExpr(java.util.Map, platform.server.session.Modifier)) && target(property) && args(map, modifier)")
    public Object callGetExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map map, Modifier modifier) throws Throwable {
        return callAutoHint(thisJoinPoint, property, modifier);
    }
    @Around("execution(* platform.server.logics.property.Property.getIncrementChange(platform.server.session.Modifier)) && target(property) && args(modifier)")
    public Object callGetIncrementChange(ProceedingJoinPoint thisJoinPoint, Property property, Modifier modifier) throws Throwable {
        return callAutoHint(thisJoinPoint, property, modifier);
    }

    private static boolean allowHint(Property property) {
        FormInstance catchHint = catchAutoHint.get();
        return catchHint!=null && !catchHint.isHintIncrement(property) && catchHint.allowHintIncrement(property); // неправильно так как может быть не changed
    } 
    @Around("execution(* platform.server.logics.property.Property.getQuery(platform.server.session.PropertyChanges,platform.server.logics.property.PropertyQueryType,java.util.Map)) && target(property) && args(propChanges, queryType, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges, PropertyQueryType queryType, Map interfaceValues) throws Throwable {
        assert property.isFull();

        IQuery<?, String> result = (IQuery) thisJoinPoint.proceed();
        if(queryType == PropertyQueryType.RECURSIVE)
            return result;

        if(allowHint(property)) { // неправильно так как может быть не changed
            Expr expr = result.getExpr("value");
            long exprComplexity = expr.getComplexity(false);

            Where changed = null; long whereComplexity = 0;
            if(queryType.needChange()) {
                changed = result.getExpr("changed").getWhere();
                whereComplexity = changed.getComplexity(false);
            }

            long complexity = max(exprComplexity, whereComplexity);
            if(complexity > Settings.instance.getLimitHintIncrementComplexity() && property.hasChanges(propChanges)) // сложность большая, если нет изменений то ничем не поможешь
                if(interfaceValues.isEmpty() && queryType == PropertyQueryType.FULLCHANGED) {
                    Map<?, KeyExpr> mapKeys = result.getMapKeys();
                    Expr prevExpr = property.getExpr(mapKeys);
                    if(whereComplexity > Settings.instance.getLimitHintIncrementComplexity() || exprComplexity > prevExpr.getComplexity(false) * Settings.instance.getLimitGrowthIncrementComplexity()) {
                        if (changed.getStatKeys(mapKeys.values()).rows.lessEquals(new Stat(Settings.instance.getLimitHintIncrementStat())))
                            throw new AutoHintException(property, true);
                        if(complexity > Settings.instance.getLimitHintNoUpdateComplexity()) {
                            System.out.println("AUTO HINT NOUPDATE" + property);
                            throw new AutoHintException(property, false);
                        }
                    }
                } else // запускаем getQuery уже без interfaceValues, соответственно уже оно если надо (в смысле что статистика будет нормальной) кинет exception
                    property.getQuery(propChanges, PropertyQueryType.FULLCHANGED, new HashMap());
        }
        return result;
    }


    // aspect который ловит getExpr'ы и оборачивает их в query, для mapKeys после чего join'ит их чтобы импользовать кэши
    @Around("execution(* platform.server.logics.property.Property.getJoinExpr(java.util.Map,platform.server.session.PropertyChanges,platform.server.data.where.WhereBuilder)) " +
            "&& target(property) && args(joinExprs,propChanges,changedWhere)")
    public Object callGetJoinExpr(ProceedingJoinPoint thisJoinPoint, Property property, Map joinExprs, PropertyChanges propChanges, WhereBuilder changedWhere) throws Throwable {
        // сначала target в аспекте должен быть
        if(!property.isFull() || !allowHint(property))
            return thisJoinPoint.proceed();

        WhereBuilder cascadeWhere = Property.cascadeWhere(changedWhere);
        Expr result = (Expr) thisJoinPoint.proceed(new Object[]{property, joinExprs, propChanges, cascadeWhere});

        long complexity = max(result.getComplexity(false), (changedWhere != null ? cascadeWhere.toWhere().getComplexity(false) : 0));
        if(complexity > Settings.instance.getLimitHintIncrementComplexity() && property.hasChanges(propChanges))
            property.getQuery(propChanges, PropertyQueryType.FULLCHANGED, new HashMap()); // по аналогии с верхним
        
        if(changedWhere!=null) changedWhere.add(cascadeWhere.toWhere());
        return result;
    }

    public static class AutoHintException extends RuntimeException {

        public final Property property;
        public final boolean lowstat; 
        public AutoHintException(Property property, boolean lowstat) {
            this.property = property;
            this.lowstat = lowstat;
        }
    }
}
