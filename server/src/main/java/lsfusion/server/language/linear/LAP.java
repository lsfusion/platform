package lsfusion.server.language.linear;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PropertyUtils;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.action.flow.CaseActionProperty;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.ListActionProperty;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.ExecutionEnvironment;

import java.sql.SQLException;
import java.util.List;

public class LAP<T extends PropertyInterface> extends LP<T, ActionProperty<T>> {

    public LAP(ActionProperty<T> property) {
        super(property);
    }

    public LAP(ActionProperty<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public void execute(DataSession session, ExecutionStack stack, ObjectValue... objects) throws SQLException, SQLHandledException {
        execute((ExecutionEnvironment)session, stack, objects);
    }

    public void execute(ExecutionEnvironment session, ExecutionStack stack, ObjectValue... objects) throws SQLException, SQLHandledException {
        property.execute(getMapValues(objects), session, stack, null);
    }

    public FlowResult execute(ExecutionContext<?> context, ObjectValue... objects) throws SQLException, SQLHandledException {
        return property.execute(context.override(getMapValues(objects), (FormEnvironment<T>) null));
    }

    public <X extends PropertyInterface> FlowResult execute(ExecutionContext<X> context) throws SQLException, SQLHandledException {
        return property.execute(BaseUtils.<ExecutionContext<T>>immutableCast(context.override(MapFact.<X, ObjectValue>EMPTY())));
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, IncrementType type, Event event, LCP<P> lp, Integer... mapping) {
        lm.addEventAction(property, new CalcPropertyMapImplement<>(lp.property.getChanged(type, event.getScope()), lp.getRevMap(listInterfaces, mapping)), MapFact.<CalcPropertyInterfaceImplement<T>, Boolean>EMPTYORDER(), false, event, false, null);
    }

    public ValueClass[] getInterfaceClasses() { // obsolete
        return listInterfaces.mapList(property.getInterfaceClasses(ClassType.obsolete)).toArray(new ValueClass[listInterfaces.size()]); // тут все равно obsolete
    }

    public ValueClass[] getInterfaceClasses(ClassType classType) {
        return property.getInterfaceClasses(listInterfaces, classType);
    }

    public <U extends PropertyInterface> ActionPropertyMapImplement<T, U> getImplement(U... mapping) {
        return new ActionPropertyMapImplement<>(property, getRevMap(mapping));
    }

    public <P extends PropertyInterface> void addToContextMenuFor(LP<P, Property<P>> mainProperty, LocalizedString contextMenuCaption) {
        mainProperty.property.setContextMenuAction(property.getSID(), contextMenuCaption);
    }

    public <P extends PropertyInterface> void setAsEditActionFor(String actionSID, LP<P, Property<P>> mainProperty) {
        assert listInterfaces.size() <= mainProperty.listInterfaces.size();

        //мэпим входы по порядку, у этого экшна входов может быть меньше
        ActionPropertyMapImplement<T, P> actionImplement = new ActionPropertyMapImplement<>(property, getRevMap(mainProperty.listInterfaces));

        mainProperty.property.setEditAction(actionSID, actionImplement);
    }

    public void addOperand(boolean hasWhen, List<ResolveClassSet> signature, Version version, Object... params) {
        ImList<PropertyInterfaceImplement<T>> readImplements = PropertyUtils.readImplements(listInterfaces, params);
        ActionPropertyMapImplement<?, PropertyInterface> actImpl = (ActionPropertyMapImplement<?, PropertyInterface>)readImplements.get(0);
        if (property instanceof ListActionProperty) {
            ((ListActionProperty) property).addAction(actImpl, version);
        } else if (hasWhen) {
            ((CaseActionProperty) property).addCase((CalcPropertyMapImplement<?, PropertyInterface>)readImplements.get(1), actImpl, version);
        } else {
            ((CaseActionProperty) property).addOperand(actImpl, signature, version);
        }
    }
}
