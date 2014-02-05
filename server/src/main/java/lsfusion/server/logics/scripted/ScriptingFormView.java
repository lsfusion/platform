package lsfusion.server.logics.scripted;

import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.*;
import lsfusion.server.logics.scripted.proxy.ViewProxyUtil;

import java.util.List;

public class ScriptingFormView {

    private FormView view;
    private final ScriptingLogicsModule LM;
    private final ScriptingErrorLog errLog;
    private final ScriptParser parser;

    public ScriptingFormView(FormView view, ScriptingLogicsModule iLM) {
        this.LM = iLM;
        this.errLog = LM.getErrLog();
        this.parser = LM.getParser();
        this.view = view;
    }

    public GroupObjectView addGroupObject(GroupObjectEntity groupObject) {
        return view.addGroupObject(groupObject);
    }

    public ComponentView getComponentBySID(String sid) throws ScriptingErrorLog.SemanticErrorException {
        return getComponentBySID(sid, true);
    }

    public ComponentView getComponentBySID(String sid, boolean hasToExist) throws ScriptingErrorLog.SemanticErrorException {
        ComponentView component = view.getComponentBySID(sid);
        if (hasToExist && component == null) {
            errLog.emitComponentNotFoundError(parser, sid);
        }

        return component;
    }

    public ContainerView getParentContainer(ComponentView child) throws ScriptingErrorLog.SemanticErrorException {
        if (child == null) {
            errLog.emitComponentIsNullError(parser, "can't get parent:");
        }

        return child.getContainer();
    }

    public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
        try {
            ViewProxyUtil.setObjectProperty(propertyReceiver, propertyName, propertyValue);
        } catch (Exception e) {
            errLog.emitUnableToSetPropertyError(parser, propertyName, e.getMessage());
        }
    }

    public ContainerView createNewComponent(String sid, ComponentView parentComponent, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent) throws ScriptingErrorLog.SemanticErrorException {
        assert sid != null && sid.matches("[a-zA-Z][a-zA-Z_0-9]*(\\.[a-zA-Z][a-zA-Z_0-9]*)*") && parentComponent != null;

        if (getComponentBySID(sid, false) != null) {
            errLog.emitAlreadyDefinedError(parser, "component", sid);
        }

        ContainerView container = view.createContainer(null, null, sid);
        if (parentComponent != null) {
            moveComponent(container, parentComponent, pos, anchorComponent);
        }

        return container;
    }

    public void moveComponent(ComponentView component, ComponentView parentComponent, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null;

        if (!(parentComponent instanceof ContainerView)) {
            errLog.emitComponentMustBeAContainerError(parser);
        }
        ContainerView parent = (ContainerView) parentComponent;

        if (anchorComponent != null && !parent.equals(anchorComponent.getContainer())) {
            errLog.emitIllegalInsertBeforeAfterComponentElement(parser, component.getSID(), parent.getSID(), anchorComponent.getSID());
        }

        if (component instanceof ContainerView) {
            ContainerView container = (ContainerView) component;
            if (container.isAncestorOf(parent)) {
                errLog.emitIllegalMoveComponentToSubcomponent(parser, container.getSID(), parent.getSID());
            }
        }

        switch (pos) {
            case IN:
                parent.add(component);
                break;
            case BEFORE:
                parent.addBefore(component, anchorComponent);
                break;
            case AFTER:
                parent.addAfter(component, anchorComponent);
                break;
            case FIRST:
                parent.addFirst(component);
                break;
        }
    }

    public void removeComponent(ComponentView component, boolean cascade) throws ScriptingErrorLog.SemanticErrorException {
        if (component == null) {
            errLog.emitComponentIsNullError(parser, "can't remove component:");
        }

        if (component == view.mainContainer) {
            errLog.emitRemoveMainContainerError(parser);
        }

        component.getContainer().remove(component);

        //не удаляем компоненты (не-контейнеры) из пула, чтобы можно было опять их использовать в настройке
        if (component instanceof ContainerView) {
            view.removeContainerFromMapping((ContainerView) component);
            if (cascade) {
                for (ComponentView child : ((ContainerView) component).getChildren()) {
                    removeComponent(child, true);
                }
            }
        }
    }

    public GroupObjectView getGroupObject(String sid) throws ScriptingErrorLog.SemanticErrorException {
        GroupObjectEntity groupObjectEntity = view.entity.getGroupObject(sid);
        if (groupObjectEntity == null) {
            errLog.emitComponentNotFoundError(parser, sid);
        }

        return view.getGroupObject(groupObjectEntity);
    }

    public PropertyDrawView getPropertyView(String name) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, name);
        return view.get(drawEntity);
    }

    public PropertyDrawView getPropertyView(String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, PropertyDrawEntity.createSID(name, mapping));
        return view.get(drawEntity);
    }
    
    public PropertyDrawView getPropertyView(ScriptingLogicsModule.PropertyUsage pUsage, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, pUsage, mapping);
        return view.get(drawEntity);
    }

    public ContainerView getMainContainer() {
        return view.mainContainer;
    }

    public FormView getView() {
        return view;
    }
}
