package platform.server.caches;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.reflect.MethodSignature;
import platform.base.BaseUtils;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.logics.property.Property;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;

import java.util.HashMap;
import java.util.Map;

@Aspect
public class AutoHintsAspect {

    public static ThreadLocal<FormInstance> catchAutoHint = new ThreadLocal<FormInstance>();
    
    public Object callAutoHint(ProceedingJoinPoint thisJoinPoint, Property property, Modifier modifier) throws Throwable {
        if(modifier instanceof FormInstance) { // && property.hasChanges(modifier) иначе в рекурсию уходит при changeModifier'е, надо было бы внутрь перенести
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

    @Around("execution(* platform.server.logics.property.Property.getQuery(platform.server.session.PropertyChanges,java.lang.Boolean,java.util.Map)) && target(property) && args(propChanges, changedWhere, interfaceValues)")
    public Object callGetQuery(ProceedingJoinPoint thisJoinPoint, Property property, PropertyChanges propChanges, Boolean changedWhere, Map interfaceValues) throws Throwable {

        Query<?, String> result = (Query) thisJoinPoint.proceed();

        FormInstance catchHint = catchAutoHint.get();
        if(catchHint!=null && !catchHint.isHintIncrement(property)) { // неправильно так как может быть не changed
            Expr expr = result.getExpr("value");
            long exprComplexity = expr.getComplexity(false);

            Where changed = null; long whereComplexity = 0;
            if(changedWhere) {
                changed = result.getExpr("changed").getWhere();
                whereComplexity = changed.getComplexity(false);
            }

            long complexity = BaseUtils.max(exprComplexity, whereComplexity);
            if(complexity > Settings.instance.getLimitHintIncrementComplexity() && property.hasChanges(propChanges)) // сложность большая, если нет изменений то ничем не поможешь
                if(interfaceValues.isEmpty() && changedWhere) {
                    Expr prevExpr = property.getExpr(result.mapKeys);
                    if(whereComplexity > Settings.instance.getLimitHintIncrementComplexity() || exprComplexity > prevExpr.getComplexity(false) * Settings.instance.getLimitGrowthIncrementComplexity()) {
                        Where incrementWhere = changed.and(expr.getWhere().or(prevExpr.getWhere()));
                        if(incrementWhere.getStatKeys(result.getInnerKeys()).rows.less(new Stat(Settings.instance.getLimitHintIncrementStat())))
                            throw new AutoHintException(property, true);
                        if(complexity > Settings.instance.getLimitHintNoUpdateComplexity())
                            throw new AutoHintException(property, false);
                    }
                } else // запускаем getQuery уже без interfaceValues, соответственно уже оно если надо (в смысле что статистика будет нормальной) кинет exception
                    property.getQuery(propChanges, true, new HashMap());
        }
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
