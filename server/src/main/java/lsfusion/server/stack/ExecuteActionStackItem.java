package lsfusion.server.stack;

import lsfusion.base.BaseUtils;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.physics.admin.profiler.ActionProfileObject;
import lsfusion.server.physics.admin.profiler.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;

import static lsfusion.server.context.ThreadLocalContext.localize;

public class ExecuteActionStackItem extends ExecutionStackItem {
    private final ActionProperty property;

    public ExecuteActionStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint, Profiler.PROFILER_ENABLED ? new ActionProfileObject((ActionProperty) joinPoint.getTarget()) : null);
        this.property = (ActionProperty) joinPoint.getTarget();
    }
    
    public ActionProperty getProperty() {
        return property;
    }
    
    public String getCaption() {
        return BaseUtils.nullEmpty(localize(property.caption));
    }

    public String getCanonicalName() {
        return BaseUtils.nullEmpty(property.getCanonicalName());
    }
    
    public DebugInfo getDebugInfo() {
        return property.getDebugInfo();
    }

    public ActionDelegationType getDelegationType() {
        return property.getDelegationType(false);
    }
    
    public boolean isInDelegate() {
        return getDelegationType() == ActionDelegationType.IN_DELEGATE;
    }

    public boolean hasNoDebugInfo() {
        return property.getDebugInfo() == null && !property.isNamed();
    }
    
    @Override
    public String toString() {
        return localize("{message.execute.action}") + (" : " + property);
    }
}
