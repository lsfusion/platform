package lsfusion.server.language.form.design;

import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.ScriptParser;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.form.ScriptingFormEntity;
import lsfusion.server.language.proxy.ViewProxyUtil;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.FilterView;
import lsfusion.server.logics.form.interactive.design.object.GridView;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.debug.DebugInfo;

import java.util.List;
import java.util.function.Supplier;

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

    public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue, Supplier<DebugInfo.DebugPoint> debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        try {
            ViewProxyUtil.setObjectProperty(propertyReceiver, propertyName, propertyValue, debugPoint);
        } catch (Exception e) {
            errLog.emitUnableToSetPropertyError(parser, propertyName, e.getMessage());
        }
    }

    public ContainerView createNewComponent(String sid, ComponentView parentComponent, ComplexLocation<ComponentView> location, Version version, DebugInfo.DebugPoint debugPoint) throws ScriptingErrorLog.SemanticErrorException {
        assert sid != null && sid.matches("[a-zA-Z][a-zA-Z_0-9]*(\\.[a-zA-Z][a-zA-Z_0-9]*)*") && parentComponent != null;

        if (getComponentBySID(sid, false, version) != null) {
            errLog.emitAlreadyDefinedError(parser, "component", sid);
        }

        ContainerView container = view.createContainer(null, sid, sid, version, debugPoint);

        addOrMoveComponent(container, parentComponent, location, version);

        return container;
    }

    public void moveComponent(ComponentView component, ComponentView parentComponent, ComplexLocation<ComponentView> location, Version version) throws ScriptingErrorLog.SemanticErrorException {
        addOrMoveComponent(component, parentComponent, location, version);
    }

    public void addOrMoveComponent(ComponentView component, ComponentView parentComponent, ComplexLocation<ComponentView> location, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null && parentComponent != null;

        if(location == null)
            location = ComplexLocation.DEFAULT();

        if(parentComponent instanceof GridView) {
            parentComponent = ((GridView) parentComponent).getNFRecord(view);
        }

        if (!(parentComponent instanceof ContainerView))
            errLog.emitComponentMustBeAContainerError(parser, parentComponent.getSID());

        if (component instanceof PropertyDrawView && ((PropertyDrawView) component).entity.isNFList(view.entity, version)) {
            errLog.emitIllegalGridPropertyMoveError(parser, component.getSID());
        }

        if (component instanceof ContainerView) {
            ContainerView container = (ContainerView) component;
            if (container.isNFAncestorOf(parentComponent, version)) {
                errLog.emitIllegalMoveComponentToSubcomponentError(parser, container.getSID(), parentComponent.getSID());
            }
        }

        ComponentView incorrectNeighbour = ((ContainerView) parentComponent).addOrMoveChecked(component, location, version);
        if(incorrectNeighbour != null)
            errLog.emitIllegalInsertBeforeAfterElementError(parser, component.getSID(), parentComponent.getSID(), incorrectNeighbour.getSID());
    }

    public void removeComponent(ComponentView component, Version version) throws ScriptingErrorLog.SemanticErrorException {
        assert component != null;
        
        if (component == view.mainContainer) {
            errLog.emitRemoveMainContainerError(parser);
        }

        view.removeComponent(component, version);
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

    public FilterView getFilterView(String name, Version version) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, name, version);
        return view.getFilter(drawEntity);
    }

    public FilterView getFilterView(String name, List<String> mapping, Version version) throws ScriptingErrorLog.SemanticErrorException {
        PropertyDrawEntity drawEntity = ScriptingFormEntity.getPropertyDraw(LM, view.entity, PropertyDrawEntity.createSID(name, mapping), version);
        return view.getFilter(drawEntity);
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
