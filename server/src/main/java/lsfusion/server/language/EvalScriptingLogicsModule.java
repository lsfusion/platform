package lsfusion.server.language;

import lsfusion.base.Pair;
import lsfusion.interop.navigator.NavigatorScheduler;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.table.IndexType;
import lsfusion.server.language.action.ActionSettings;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.navigator.window.NavigatorWindowOptions;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.PropertySettings;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.Event;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.NavigatorEntity;
import lsfusion.server.logics.property.LazyProperty;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.debug.PropertyFollowsDebug;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// a module made of a text while the server runs, after the logics is final : what it declares is its own, and what would
// change the logics is refused
public class EvalScriptingLogicsModule extends ScriptingLogicsModule {
    private Set<EvalScriptingLogicsModule> parentLMs;

    // whether the code changes the navigator - a copy of its own : the code that customizes the application does, a script
    // that is run (EVAL, the code that customizes a form) does not - nothing would keep its change. The caller says it,
    // until a code can declare a navigator of its own as it declares a form (FORM ... EXTEND FORMS)
    private final boolean changesNavigator;

    // the copy, made as the code first names the navigator, an element of it or a window - the root is asked for by the
    // first pass over a NAVIGATOR statement already, it is the argument of a rule; the module has its place among the
    // modules by then, and a copy is made at it. A code that names none - one that only declares what a call through the
    // Action API runs, say - copies nothing. Only the compile of the code makes it, in the thread that compiles : once the
    // code is compiled the module's navigator is settled (finalizeAndCheckNavigator) and is only read, by every thread
    private NavigatorEntity navigator;

    public EvalScriptingLogicsModule(BaseLogicsModule baseModule, BusinessLogics BL, Set<EvalScriptingLogicsModule> parentLMs, String code, boolean changesNavigator) {
        super(code, baseModule, BL);
        this.parentLMs = parentLMs;
        this.changesNavigator = changesNavigator;
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

    // the statements of a code that changes the navigator write into its copy of the logics' navigator, which is final and
    // would refuse them. And every name they give is found in the copy : the element a statement changes or moves, the one
    // it puts another before or after, the window it hides or turns over, the one an element is put into. So none gets
    // what is the logics'. Once the code is compiled the copy is what a client the code customizes draws.
    // A script changes nothing of the navigator : it finds the window it names in the navigator of the code it is run
    // under - a parent or a parent's parent, which the code that customizes the application changed -, or in the logics'
    @Override
    public NavigatorEntity getNavigator() {
        if (changesNavigator) {
            if (navigator == null)
                navigator = new NavigatorEntity(BL.getNavigator(), getVersion());
            return navigator;
        }

        NavigatorEntity logicsNavigator = super.getNavigator();
        if(parentLMs != null) {
            for (EvalScriptingLogicsModule parentLM : parentLMs) {
                NavigatorEntity navigator = parentLM.getNavigator();
                if (navigator != logicsNavigator)
                    return navigator;
            }
        }
        return logicsNavigator;
    }

    // the copy is checked as the logics' own navigator is as the server starts; a code that did not touch the navigator
    // leaves the logics' own to the module
    public void finalizeAndCheckNavigator() {
        if (navigator != null)
            navigator.finalizeAndCheck(baseLM);
        else
            navigator = BL.getNavigator();
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

    // the cases of an abstract are finalized only with the whole logics, which an eval module is never part of
    @Override
    public LP<?> addScriptedAbstractProp(CaseUnionProperty.Type type, String returnClass, ImList<ValueClass> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast, boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("ABSTRACT operator");
        return null;
    }

    @Override
    public LA addScriptedAbstractAction(ListCaseAction.AbstractType type, ImList<ValueClass> paramClasses, boolean isExclusive, boolean isChecked, boolean isLast, String returnClassName, List<String> returnClassNames) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("ABSTRACT operator");
        return null;
    }

    @Override
    public void addImplementationToAbstractProp(NamedPropertyUsage abstractPropUsage, List<TypedParameter> context, LPWithParams implement, LPWithParams when) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("+= statement");
    }

    @Override
    public void addImplementationToAbstractAction(NamedPropertyUsage abstractPropUsage, List<TypedParameter> context, LAWithParams implement, LPWithParams when, boolean optimisticAsync) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("ACTION+ statement");
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
        throw new ScriptParsingException(constructErrorMessage("event creation is forbidden in EVAL module"));
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
                throw new ScriptParsingException(constructErrorMessage("addition of property or action to a group from another module is forbidden in EVAL module"));
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
    
    // the navigator's schedulers and layout, what draws a window, and what other modules' properties and actions run are
    // the whole server's : a script would change them for every user, and what it puts there - a scheduler's action, a
    // window's property, an ASON handler, a BEFORE / AFTER aspect - would run in their sessions. The navigator's elements
    // and windows are the whole server's as well : a NAVIGATOR or a WINDOW statement is refused unless the code changes a
    // copy of its own (changesNavigator)
    @Override
    public void setWindowCustom(String name, String custom, LPWithParams customProperty) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("CUSTOM option of a WINDOW statement");
    }

    // a new window's CUSTOM option, as an existing window's : the standard windows are styled by what draws the logics' ones
    @Override
    public void addScriptedWindow(boolean isNative, boolean isForms, boolean single, Integer closeDelay, String name, LocalizedString captionStr, NavigatorWindowOptions options) throws ScriptingErrorLog.SemanticErrorException {
        if (!changesNavigator)
            emitEvalError("WINDOW statement");
        if (options.custom != null || options.customProperty != null)
            emitEvalError("CUSTOM option of a WINDOW statement");

        super.addScriptedWindow(isNative, isForms, single, closeDelay, name, captionStr, options);
    }

    @Override
    public void setWindowFormsKind(String name, Boolean single, Integer closeDelay) throws ScriptingErrorLog.SemanticErrorException {
        if (!changesNavigator)
            emitEvalError("WINDOW statement");
        super.setWindowFormsKind(name, single, closeDelay);
    }

    @Override
    public void hideWindow(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (!changesNavigator)
            emitEvalError("HIDE WINDOW statement");
        super.hideWindow(name);
    }

    @Override
    public NavigatorElement createScriptedNavigatorElement(String name, LocalizedString caption, DebugInfo.DebugPoint point,
                                                           NamedPropertyUsage actionUsage, String formName, boolean isAction) throws ScriptingErrorLog.SemanticErrorException {
        if (!changesNavigator)
            emitEvalError("NAVIGATOR statement");
        return super.createScriptedNavigatorElement(name, caption, point, actionUsage, formName, isAction);
    }

    @Override
    public void setupNavigatorElement(NavigatorElement element, LocalizedString caption, NavigatorElement parentElement, NavigatorElementOptions options, boolean isEditOperation) throws ScriptingErrorLog.SemanticErrorException {
        if (!changesNavigator)
            emitEvalError("NAVIGATOR statement");
        super.setupNavigatorElement(element, caption, parentElement, options, isEditOperation);
    }

    @Override
    public void setNavigatorCustom(String custom, LPWithParams customProperty) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("NAVIGATOR CUSTOM option");
    }

    @Override
    public void addNavigatorScheduler(LA action, NavigatorScheduler scheduler) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError("SCHEDULE in a NAVIGATOR statement");
    }

    // the logics' base module keeps the action that opens a form from the navigator, one for a form and for as long as
    // the server runs. An element an evaluated code declares gets an action of its own : its form can be the code's
    @Override
    protected LA<?> getNavigatorAction(FormEntity form) {
        return addNavigatorFormAction(form);
    }

    @Override
    public void addSettingsToAction(LA action, String name, LocalizedString caption, List<TypedParameter> params, List<ResolveClassSet> signature, ActionSettings as) throws ScriptingErrorLog.SemanticErrorException {
        if (!as.actionEditEvents.isEmpty())
            emitEvalError("ASON option");
        super.addSettingsToAction(action, name, caption, params, signature, as);
    }

    @Override
    public void addScriptedAspect(NamedPropertyUsage mainPropUsage, List<TypedParameter> mainPropParams, LAWithParams action, boolean before) throws ScriptingErrorLog.SemanticErrorException {
        emitEvalError(before ? "BEFORE statement" : "AFTER statement");
    }

    protected void emitEvalError(String errorPrefix) throws ScriptingErrorLog.SemanticErrorException {
        errLog.emitEvalModuleError(parser, errorPrefix);
    }
}