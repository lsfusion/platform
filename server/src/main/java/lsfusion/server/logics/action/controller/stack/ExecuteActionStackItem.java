package lsfusion.server.logics.action.controller.stack;

import lsfusion.base.BaseUtils;
import lsfusion.server.base.controller.stack.ExecutionStackItem;
import lsfusion.server.logics.action.Action;
import lsfusion.server.physics.admin.profiler.ActionProfileObject;
import lsfusion.server.physics.admin.profiler.Profiler;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.debug.DebugInfo;
import org.aspectj.lang.ProceedingJoinPoint;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class ExecuteActionStackItem extends ExecutionStackItem {
    private final Action action;

    public ExecuteActionStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint, Profiler.PROFILER_ENABLED ? new ActionProfileObject((Action) joinPoint.getTarget()) : null);
        this.action = (Action) joinPoint.getTarget();
    }
    
    public Action getAction() {
        return action;
    }
    
    public String getCaption() {
        return BaseUtils.nullEmpty(localize(action.caption));
    }

    public String getCanonicalName() {
        return BaseUtils.nullEmpty(action.getCanonicalName());
    }
    
    public DebugInfo getDebugInfo() {
        return action.getDebugInfo();
    }

    public ActionDelegationType getDelegationType() {
        return action.getDelegationType(false);
    }
    
    public boolean isInDelegate() {
        return getDelegationType() == ActionDelegationType.IN_DELEGATE;
    }

    public boolean hasNoDebugInfo() {
        return action.getDebugInfo() == null && !action.isNamed();
    }
    
    @Override
    public String toString() {
        return localize("{message.execute.action}") + (" : " + action);
    }
}
