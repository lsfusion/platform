package lsfusion.server.language.form.design;

import lsfusion.server.base.version.Version;
import lsfusion.server.language.ScriptParser;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.form.ScriptingFormEntity;
import lsfusion.server.language.proxy.ViewProxyUtil;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.object.GridView;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

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

    private ComponentView getComponentBySID(String sid, boolean hasToExist, Version version) throws ScriptingErrorLog.SemanticErrorException {
        ComponentView component = view.getComponentBySID(sid, version);
        if (hasToExist && component == null) {
            errLog.emitComponentNotFoundError(parser, sid);
        }

        return component;
    }

    public ContainerView getParentContainer(ComponentView child, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert child != null;
        ContainerView parent = child.getNFContainer(version);
        if (parent == null) {
            errLog.emitComponentParentError(parser, child.getSID());
        }
        return parent;
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

        ContainerView container = view.createContainer(null, sid, version);
        
        moveComponent(container, parentComponent, pos, anchorComponent, version);

        return container;
    }

    public void moveComponent(ComponentView component, ComponentView parentComponent, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null && parentComponent != null;

        if(parentComponent instanceof GridView) {
            moveComponentToGrid(component, (GridView) parentComponent, version);
            return;
        }

        if (parentComponent instanceof ContainerView) {
            moveComponentToContainer(component, (ContainerView) parentComponent, pos, anchorComponent, version);
            return;
        }
        errLog.emitComponentMustBeAContainerError(parser, parentComponent.getSID());
    }

    public void moveComponentToGrid(ComponentView component, GridView parentComponent, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null && parentComponent != null;

        component.removeFromParent(version);
        parentComponent.setRecord(component, version);
    }

    public void moveComponentToContainer(ComponentView component, ContainerView parent, ScriptingLogicsModule.InsertPosition pos, ComponentView anchorComponent, Version version) throws ScriptingErrorLog.SemanticErrorException {

        if (anchorComponent != null && !parent.equals(anchorComponent.getNFContainer(version))) {
            errLog.emitIllegalInsertBeforeAfterElementError(parser, component.getSID(), parent.getSID(), anchorComponent.getSID());
        }

        if (component instanceof ContainerView) {
            ContainerView container = (ContainerView) component;
            if (container.isNFAncestorOf(parent, version)) {
                errLog.emitIllegalMoveComponentToSubcomponentError(parser, container.getSID(), parent.getSID());
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

    public void removeComponent(ComponentView component, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null;
        
        if (component == view.mainContainer) {
            errLog.emitRemoveMainContainerError(parser);
        }

        component.removeFromParent(version);
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

    public PropertyDrawView getPropertyView(String name, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, PropertyDrawEntity.createSID(name, mapping), version);
        return view.get(drawEntity);
    }

    public PropertyObjectEntity addPropertyObject(ScriptingLogicsModule.AbstractFormPropertyUsage property) throws ScriptingErrorLog.SemanticErrorException {
        return ScriptingFormEntity.addPropertyObject(LM, view.entity, property);
    }

    public List<ScriptingLogicsModule.TypedParameter> getTypedObjectsNames(Version version) {
        return ScriptingFormEntity.getTypedObjectsNames(LM, view.entity, version);
    }


    public ContainerView getMainContainer() {
        return view.mainContainer;
    }

    public FormView getView() {
        return view;
    }
}
