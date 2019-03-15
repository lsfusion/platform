package lsfusion.server.base.stack;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.action.Action;
import lsfusion.server.physics.admin.profiler.ActionProfileObject;
import lsfusion.server.physics.admin.profiler.Profiler;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.debug.DebugInfo;
import org.aspectj.lang.ProceedingJoinPoint;

import static lsfusion.server.base.thread.ThreadLocalContext.localize;

public class ExecuteActionStackItem extends ExecutionStackItem {
    private final Action property;

    public ExecuteActionStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint, Profiler.PROFILER_ENABLED ? new ActionProfileObject((Action) joinPoint.getTarget()) : null);
        this.property = (Action) joinPoint.getTarget();
    }
    
    public Action getProperty() {
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
