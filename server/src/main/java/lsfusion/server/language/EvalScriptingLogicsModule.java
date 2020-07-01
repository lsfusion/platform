package lsfusion.server.language;

import lsfusion.base.Pair;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.PropertySettings;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.List;

public class EvalScriptingLogicsModule extends ScriptingLogicsModule {
    public EvalScriptingLogicsModule(BaseLogicsModule baseModule, BusinessLogics BL, String code) {
        super(code, baseModule, BL);
    }

    @Override
    public void addScriptedClass(String className, LocalizedString captionStr, boolean isAbstract,
                                 List<String> instNames, List<LocalizedString> instCaptions, List<String> parentNames, boolean isComplex,
                                 DebugInfo.DebugPoint point) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("CLASS statement");
    }

    @Override
    public void addScriptedIndex(List<TypedParameter> params, List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("INDEX statement");
    }

    @Override
    public void addScriptedTable(String name, List<String> classIds, boolean isFull, boolean isExplicit) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("TABLE statement");
    }

    @Override
    public void addScriptedConstraint(LP<?> property, Event event, boolean checked, List<NamedPropertyUsage> propUsages,
                                      LP<?> messageProperty, List<LPWithParams> properties, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("CONSTRAINT statement");
    }

    @Override
    public void addScriptedEvent(LPWithParams whenProp, LAWithParams event, List<LPWithParams> orders, boolean descending, 
                                 Event baseEvent, List<LPWithParams> noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("WHEN statement");
    }

    @Override
    public void addScriptedGlobalEvent(LAWithParams event, Event baseEvent, boolean single, ActionOrPropertyUsage showDep) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("ON statement");
    }

    @Override
    public void addScriptedLoggable(List<NamedPropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("LOGGABLE statement");
    }

    @Override
    public void addScriptedWriteWhen(NamedPropertyUsage mainPropUsage, List<TypedParameter> namedParams, LPWithParams valueProp, LPWithParams whenProp, boolean action) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("<- ... WHEN statement");
    }

    @Override
    public void addScriptedFollows(NamedPropertyUsage mainPropUsage, List<TypedParameter> namedParams, List<PropertyFollowsDebug> resolveOptions, 
                                   LPWithParams rightProp, Event event, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("=> statement");
    }

    @Override
    public LPContextIndependent addScriptedAGProp(List<TypedParameter> context, String aggClassName, LPWithParams whereExpr, 
                                                  DebugInfo.DebugPoint classDebugPoint, DebugInfo.DebugPoint exprDebugPoint, boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("AGGR operator");
        return null;
    }

    @Override
    public Pair<LPWithParams, LPContextIndependent> addScriptedCDGProp(int oldContextSize, List<LPWithParams> groupProps, GroupingType type, 
                                                                       List<LPWithParams> mainProps, List<LPWithParams> orderProps, boolean ascending, 
                                                                       LPWithParams whereProp, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.AGGR) {
            emitEvalError("GROUP operator's AGGR type");            
        } else if (type == GroupingType.EQUAL) {
            emitEvalError("GROUP operator's EQUAL type");
        }
        return super.addScriptedCDGProp(oldContextSize, groupProps, type, mainProps, orderProps, ascending, whereProp, newContext);
    }    
    
    @Override
    public void addSettingsToProperty(LP<?> property, String name, LocalizedString caption, List<TypedParameter> params, 
                                      List<ResolveClassSet> signature, PropertySettings ps) throws ScriptingErrorLog.SemanticErrorException {
        if (ps.isLoggable) {
            emitEvalError("LOGGABLE property option");
        } else if (ps.isPersistent) {
            emitEvalError("MATERIALIZED property option");
        } else if (ps.table != null) {
            emitEvalError("TABLE property option");
        } else if (ps.notNull != null) {
            emitEvalError("NONULL property option");
        }
        super.addSettingsToProperty(property, name, caption, params, signature, ps);
    }    
    
    @Override
    public <P extends PropertyInterface> void addBaseEvent(Action<P> action, Event event, boolean resolve, boolean single) {
        throw new RuntimeException(constructErrorMessage("event creation is forbidden in EVAL module"));
    }

    @Override
    public void addScriptedGroup(String groupName, LocalizedString captionStr, String integrationSID, String parentName) throws ScriptingErrorLog.SemanticErrorException {
        if (parentName != null && !isNewGroup(parentName)) {
            emitEvalError("group parents from another module");    
        }
        super.addScriptedGroup(groupName, captionStr, integrationSID, parentName);
    }

    @Override
    protected void addPropertyToGroup(ActionOrProperty<?> property, Group group) {
        if (group != null && !property.isLocal()) { 
            if (isNewGroup(group)) {
                super.addPropertyToGroup(property, group);
            } else {
                throw new RuntimeException(constructErrorMessage("addition of property or action to a group from another module is forbidden in EVAL module"));
            }
        } 
    }    
    
    private String constructErrorMessage(String message) {
        String resMessage = null;
        try {
            errLog.emitSimpleError(parser, message);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            resMessage =  e.getMessage();
        }
        return resMessage;
    }
    
    private boolean isNewGroup(Group group) {
        return group.equals(getGroup(group.getName()));
    }
    
    private boolean isNewGroup(String groupName) throws ScriptingErrorLog.SemanticErrorException {
        Group group = findGroup(groupName);
        return isNewGroup(group);
    }
    
    @Override
    public LP addScriptedDProp(String returnClass, List<String> paramClasses, boolean sessionProp, boolean innerProp, boolean isLocalScope, LocalNestedType nestedType) throws ScriptingErrorLog.SemanticErrorException {
        if (!sessionProp) {
            nestedType = LocalNestedType.ALL;
        }
        return super.addScriptedDProp(returnClass, paramClasses, true, innerProp, isLocalScope, nestedType);
    }
    
    private void emitEvalError(String errorPrefix) throws ScriptingErrorLog.SemanticErrorException {
        errLog.emitEvalModuleError(parser, errorPrefix);
    }
}