package lsfusion.server.stack;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.debug.ActionDelegationType;
import lsfusion.server.logics.debug.DebugInfo;
import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.profiler.ActionProfileObject;
import lsfusion.server.profiler.Profiler;
import org.aspectj.lang.ProceedingJoinPoint;

public class ExecuteActionStackItem extends ExecutionStackItem {
    private final ActionProperty property;
    private String propertyName;

    public ExecuteActionStackItem(ProceedingJoinPoint joinPoint) {
        super(joinPoint, Profiler.PROFILER_ENABLED ? new ActionProfileObject((ActionProperty) joinPoint.getTarget()) : null);
        this.property = (ActionProperty) joinPoint.getTarget();
    }
    
    public ActionProperty getProperty() {
        return property;
    }
    
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getCaption() {
        return BaseUtils.nullEmpty(property.caption);
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
    
    @Override
    public String toString() {
        String result = ServerResourceBundle.getString("message.execute.action");
        if(propertyName != null) {
            result += " : " + propertyName;
            if(property.getDebugInfo() != null) {
                result += ":" + property.getDebugInfo();
            }
        } else {
            result += " : " + property;
        }
        return result;
    }
}
