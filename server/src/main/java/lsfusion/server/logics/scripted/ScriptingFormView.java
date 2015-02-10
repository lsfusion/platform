package lsfusion.server.logics.scripted;

import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.*;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.scripted.proxy.ViewProxyUtil;

import java.util.List;

public class ScriptingFormView {
    private final FormView view;
    private final ScriptingLogicsModule LM;
    private final ScriptingErrorLog errLog;
    private final ScriptParser parser;

    public ScriptingFormView(FormView view, ScriptingLogicsModule iLM) {
        this.LM = iLM;
        this.errLog = LM.getErrLog();
        this.parser = LM.getParser();
        this.view = view;
    }

    public ComponentView getComponentBySID(String sid, Version version) throws ScriptingErrorLog.SemanticErrorException {
        return getComponentBySID(sid, true, version);
    }

    public ComponentView getComponentBySID(String sid, boolean hasToExist, Version version) throws ScriptingErrorLog.SemanticErrorException {
        ComponentView component = view.getComponentBySID(sid, version);
        if (hasToExist && component == null) {
            errLog.emitComponentNotFoundError(parser, sid);
        }

        return component;
    }

    public ContainerView getParentContainer(ComponentView child, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (child == null) {
            errLog.emitComponentIsNullError(parser, "can't get parent:");
        }

        return child.getNFContainer(version);
    }

    public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
        try {
            ViewProxyUtil.setObjectProperty(propertyReceiver, propertyName, propertyValue);
        } catch (Exception e) {
            errLog.emitUnableToSetPropertyError(parser, propertyName, e.getMessage());
        }
    }

    public ContainerView createNewComponent(String sid, ComponentView parentComponent, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert sid != null && sid.matches("[a-zA-Z][a-zA-Z_0-9]*(\\.[a-zA-Z][a-zA-Z_0-9]*)*") && parentComponent != null;

        if (getComponentBySID(sid, false, version) != null) {
            errLog.emitAlreadyDefinedError(parser, "component", sid);
        }

        ContainerView container = view.createContainer(null, null, sid, version);
        
        moveComponent(container, parentComponent, pos, anchorComponent, version);

        return container;
    }

    public void moveComponent(ComponentView component, ComponentView parentComponent, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null;

        if (!(parentComponent instanceof ContainerView)) {
            errLog.emitComponentMustBeAContainerError(parser);
        }
        ContainerView parent = (ContainerView) parentComponent;

        if (anchorComponent != null && !parent.equals(anchorComponent.getNFContainer(version))) {
            errLog.emitIllegalInsertBeforeAfterElement(parser, component.getSID(), parent.getSID(), anchorComponent.getSID());
        }

        if (component instanceof ContainerView) {
            ContainerView container = (ContainerView) component;
            if (container.isNFAncestorOf(parent, version)) {
                errLog.emitIllegalMoveComponentToSubcomponent(parser, container.getSID(), parent.getSID());
            }
        }

        switch (pos) {
            case IN:
                parent.add(component, version);
                break;
            case BEFORE:
                parent.addBefore(component, anchorComponent, version);
                break;
            case AFTER:
                parent.addAfter(component, anchorComponent, version);
                break;
            case FIRST:
                parent.addFirst(component, version);
                break;
        }
    }

    public void removeComponent(ComponentView component, boolean cascade, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (component == null) {
            errLog.emitComponentIsNullError(parser, "can't remove component:");
        }

        if (component == view.mainContainer) {
            errLog.emitRemoveMainContainerError(parser);
        }

        component.getNFContainer(version).remove(component, version);

        //не удаляем компоненты (не-контейнеры) из пула, чтобы можно было опять их использовать в настройке
        if (component instanceof ContainerView) {
            view.removeContainerFromMapping((ContainerView) component, version);
            if (cascade) {
                for (ComponentView child : ((ContainerView) component).getNFChildrenIt(version)) {
                    removeComponent(child, true, version);
                }
            }
        }
    }

    public GroupObjectView getGroupObject(String sid, Version version) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity groupObjectEntity = view.entity.getNFGroupObject(sid, version);
        if (groupObjectEntity == null) {
            errLog.emitComponentNotFoundError(parser, sid);
        }

        return view.getNFGroupObject(groupObjectEntity, version);
    }

    public PropertyDrawView getPropertyView(String name, Version version) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, name, version);
        return view.get(drawEntity);
    }

    public PropertyDrawView getPropertyView(PropertyDrawEntity propertyDraw, Version version) {
        return view.get(propertyDraw);
    }

    public PropertyDrawView getPropertyView(String name, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, PropertyDrawEntity.createSID(name, mapping), version);
        return view.get(drawEntity);
    }

    public PropertyDrawView getPropertyView(ScriptingLogicsModule.PropertyUsage pUsage, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, pUsage, mapping, version);
        return view.get(drawEntity);
    }

    public CalcPropertyObjectEntity addCalcPropertyObject(ScriptingLogicsModule.PropertyUsage property, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        return ScriptingFormEntity.addCalcPropertyObject(LM, view.entity, property, mapping);
    }

    public ContainerView getMainContainer() {
        return view.mainContainer;
    }

    public FormView getView() {
        return view;
    }
}
