package lsfusion.server.language;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.table.IndexType;
import lsfusion.base.Result;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.PropertySettings;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.LazyProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.resolve.ResolvingErrors;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EvalScriptingLogicsModule extends ScriptingLogicsModule {
    private Set<EvalScriptingLogicsModule> parentLMs;
    public EvalScriptingLogicsModule(BaseLogicsModule baseModule, BusinessLogics BL, Set<EvalScriptingLogicsModule> parentLMs, String code) {
        super(code, baseModule, BL);
        this.parentLMs = parentLMs;
    }

    // all direct by-name references of the script, collected at compile time to be checked against the caller's
    // security policy in BaseLogicsModule.evaluateRun (script-defined elements also get here, but they are new
    // objects that can't be present in the policy maps, so they pass the check automatically)
    public final Set<ActionOrProperty> usedRefs = new LinkedHashSet<>();
    public final Set<Property> changeRefs = new LinkedHashSet<>(); // targets of CHANGE / <- / RECALCULATE
    // INTERNAL java / INTERNAL DB / EXTERNAL DB(F) defined by the script itself - can't be statically checked
    // against the property security policy (java / sql read data bypassing it)
    public boolean hasUnsafeCode;

    private boolean captureChangeRef;

    @Override
    public LP<?> resolveProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LP<?> result = super.resolveProperty(compoundName, params);
        registerPropertyRef(result);
        return result;
    }

    @Override
    public LP<?> resolveAbstractProperty(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        LP<?> result = super.resolveAbstractProperty(compoundName, params, prioritizeNotEquals);
        registerPropertyRef(result);
        return result;
    }

    @Override
    public LA<?> resolveAction(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LA<?> result = super.resolveAction(compoundName, params);
        if(result != null)
            usedRefs.add(result.action);
        return result;
    }

    @Override
    public LA<?> resolveAbstractAction(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        LA<?> result = super.resolveAbstractAction(compoundName, params, prioritizeNotEquals);
        if(result != null)
            usedRefs.add(result.action);
        return result;
    }

    private void registerPropertyRef(LP<?> result) {
        if(result != null) {
            usedRefs.add(result.property);
            if(captureChangeRef) { // the first property resolved inside getIdentityLPPropertyUsageWithWhere is the change target
                changeRefs.add(result.property);
                captureChangeRef = false;
            }
        }
    }

    @Override
    public Pair<LPWithParams, LPWithParams> getIdentityLPPropertyUsageWithWhere(List<TypedParameter> context, NamedPropertyUsage propertyUsage, List<LPWithParams> propertyMapping, LPWithParams whereProperty, List<TypedParameter> newContext, Result<Integer> rExParams) throws ScriptingErrorLog.SemanticErrorException {
        captureChangeRef = true;
        try {
            return super.getIdentityLPPropertyUsageWithWhere(context, propertyUsage, propertyMapping, whereProperty, newContext, rExParams);
        } finally {
            captureChangeRef = false;
        }
    }

    @Override
    public LA addScriptedInternalAction(String javaClassName, ImList<ValueClass> paramClasses, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
        hasUnsafeCode = true;
        return super.addScriptedInternalAction(javaClassName, paramClasses, allowNullValue);
    }

    @Override
    public LA addScriptedInternalAction(String code, boolean allowNullValue) throws ScriptingErrorLog.SemanticErrorException {
        hasUnsafeCode = true;
        return super.addScriptedInternalAction(code, allowNullValue);
    }

    @Override
    public LAWithParams addScriptedInternalDBAction(LPWithParams exec, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        hasUnsafeCode = true;
        return super.addScriptedInternalDBAction(exec, params, context, toPropertyUsageList);
    }

    @Override
    public LAWithParams addScriptedExternalDBAction(LPWithParams connectionString, LPWithParams exec, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        hasUnsafeCode = true;
        return super.addScriptedExternalDBAction(connectionString, exec, params, context, toPropertyUsageList);
    }

    @Override
    public LAWithParams addScriptedExternalDBFAction(LPWithParams connectionString, String charset, List<LPWithParams> params, List<TypedParameter> context, List<NamedPropertyUsage> toPropertyUsageList) throws ScriptingErrorLog.SemanticErrorException {
        hasUnsafeCode = true;
        return super.addScriptedExternalDBFAction(connectionString, charset, params, context, toPropertyUsageList);
    }

    @Override
    public void addScriptedClass(String className, LocalizedString captionStr, String image, boolean isAbstract,
                                 List<String> instNames, List<LocalizedString> instCaptions, List<String> images, List<String> parentNames, boolean isComplex,
                                 DebugInfo.DebugPoint point) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("CLASS statement");
    }

    @Override
    public void addScriptedIndex(String dbName, List<TypedParameter> params, List<LPWithParams> lps, IndexType indexType) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("INDEX statement");
    }

    @Override
    public void addScriptedTable(String name, String dbName, List<String> classIds, boolean isFull, boolean isExplicit) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("TABLE statement");
    }

    @Override
    <T extends PropertyInterface> void addScriptedConstraint(LP<T> property, List<TypedParameter> context, Event event, boolean checked, List<NamedPropertyUsage> propUsages, LP<?> messageProperty, List<LPWithParams> properties, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("CONSTRAINT statement");
    }

    @Override
    public void addScriptedWhen(LPWithParams whenProp, List<TypedParameter> newContext, LAWithParams event, List<LPWithParams> orders, boolean descending, Event baseEvent, List<LPWithParams> noInline, boolean forceInline, DebugInfo.DebugPoint debugPoint, LocalizedString debugCaption) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("WHEN statement");
    }

    @Override
    public void addScriptedGlobalEvent(LAWithParams event, Event baseEvent, boolean single) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("ON statement");
    }

    public void addScriptedLoggable(List<NamedPropertyUsage> propUsages) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("LOGGABLE statement");
    }

    @Override
    protected LogicsModule getSysModule(String requiredModuleName) {
        if(parentLMs != null) {
            for (EvalScriptingLogicsModule parentLM : parentLMs) {
                LogicsModule result = parentLM.getSysModule(requiredModuleName);
                if (result != null) {
                    return result;
                } else if (parentLM.getName().equals(requiredModuleName)) {
                    return parentLM;
                }
            }
        }
        return super.getSysModule(requiredModuleName);
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
    public <T extends PropertyInterface> LPContextIndependent addScriptedAGProp(List<TypedParameter> context, String aggClassName, LPWithParams whereExpr,
                                                  Event aggrEvent, DebugInfo.DebugPoint aggrDebugPoint, Event newEvent, DebugInfo.DebugPoint newDebugPoint, Event deleteEvent, DebugInfo.DebugPoint deleteDebugPoint, boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("AGGR operator");
        return null;
    }

    @Override
    public Pair<LPWithParams, LPContextIndependent> addScriptedCDGProp(int oldContextSize, List<LPWithParams> groupProps, GroupingType type,
                                                                       List<LPWithParams> mainProps, List<LPWithParams> orderProps, boolean descending,
                                                                       LPWithParams whereProp, SelectTop<LPWithParams> selectTop, List<TypedParameter> newContext, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.AGGR) {
            emitEvalError("GROUP operator's AGGR type");            
        } else if (type == GroupingType.EQUAL) {
            emitEvalError("GROUP operator's EQUAL type");
        }
        return super.addScriptedCDGProp(oldContextSize, groupProps, type, mainProps, orderProps, descending, whereProp, selectTop, newContext, debugPoint);
    }    
    
    @Override
    public <K extends PropertyInterface> void addSettingsToProperty(LP<K> property, String name, LocalizedString caption, List<TypedParameter> params,
                                                                    List<ResolveClassSet> signature, PropertySettings ps, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        if (ps.isMaterialized) {
            emitEvalError("MATERIALIZED property option");
        } else if (ps.table != null) {
            emitEvalError("TABLE property option");
        } else if (ps.notNull != null) {
            emitEvalError("NONULL property option");
        }
        super.addSettingsToProperty(property, name, caption, params, signature, ps, debugPoint);
    }    
    
    @Override
    public <P extends PropertyInterface> void addBaseEvent(Action<P> action, Event event, boolean single) {
        throw new RuntimeException(constructErrorMessage("event creation is forbidden in EVAL module"));
    }

    @Override
    public void addScriptedGroup(String groupName, LocalizedString captionStr, String integrationSID, String parentName, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        if (parentName != null && !isNewGroup(parentName)) {
            emitEvalError("group parents from another module");    
        }
        super.addScriptedGroup(groupName, captionStr, integrationSID, parentName, debugPoint);
    }

    public Set<LazyProperty> lazyProps = new LinkedHashSet<>();
    @Override
    protected void addPropertyToGroup(ActionOrProperty<?> property, Group group) {
        if (group != null && !property.isLocal()) { 
            if (isNewGroup(group)) {
                super.addPropertyToGroup(property, group);
            } else {
                throw new RuntimeException(constructErrorMessage("addition of property or action to a group from another module is forbidden in EVAL module"));
            }
        }
        if(property instanceof LazyProperty)
            lazyProps.add((LazyProperty)property);
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
    public LP addScriptedDProp(String returnClass, ImList<ValueClass> paramClasses, boolean sessionProp, boolean innerProp, boolean isLocalScope, LocalNestedType nestedType) throws ScriptingErrorLog.SemanticErrorException {
//        if (!sessionProp) {
//            nestedType = LocalNestedType.ALL;
//        }
        return super.addScriptedDProp(returnClass, paramClasses, true, innerProp, isLocalScope, nestedType);
    }
    
    private void emitEvalError(String errorPrefix) throws ScriptingErrorLog.SemanticErrorException {
        errLog.emitEvalModuleError(parser, errorPrefix);
    }
}