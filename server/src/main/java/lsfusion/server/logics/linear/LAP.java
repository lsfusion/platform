package lsfusion.server.logics.linear;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.PropertyUtils;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.FormEnvironment;
import lsfusion.server.logics.property.actions.flow.CaseActionProperty;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.actions.flow.ListActionProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class LAP<T extends PropertyInterface> extends LP<T, ActionProperty<T>> {

    public LAP(ActionProperty<T> property) {
        super(property);
    }

    public LAP(ActionProperty<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public void execute(DataSession session, ObjectValue... objects) throws SQLException {
        property.execute(getMapValues(objects), session, null);
    }

    public FlowResult execute(ExecutionContext<?> context, DataObject... objects) throws SQLException {
        return property.execute(context.override(getMapValues(objects), (FormEnvironment<T>) null));
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, Event.APPLY, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, Event event, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, false, event, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventSetAction(LogicsModule lm, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, true, Event.APPLY, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, boolean changedSet, Event event, LCP<P> lp, Integer... mapping) {
        setEventAction(lm, changedSet ? IncrementType.SET : IncrementType.SETCHANGED, event, lp, mapping);
    }

    public <P extends PropertyInterface> void setEventAction(LogicsModule lm, IncrementType type, Event event, LCP<P> lp, Integer... mapping) {
        lm.addEventAction(property, new CalcPropertyMapImplement<P, T>(lp.property.getChanged(type), lp.getRevMap(listInterfaces, mapping)), MapFact.<CalcPropertyInterfaceImplement<T>, Boolean>EMPTYORDER(), false, event, false);
    }

    public ValueClass[] getInterfaceClasses() {
        return listInterfaces.mapOrder(property.getInterfaceClasses(ClassType.ASIS)).toArray(new ValueClass[listInterfaces.size()]); // тут все равно obsolete
    }

    public <U extends PropertyInterface> ActionPropertyMapImplement<T, U> getImplement(U... mapping) {
        return new ActionPropertyMapImplement<T, U>(property, getRevMap(mapping));
    }

    public <P extends PropertyInterface> void addToContextMenuFor(LP<P, Property<P>> mainProperty) {
        addToContextMenuFor(mainProperty, property.caption != null ? property.caption : property.getSID());
    }

    public <P extends PropertyInterface> void addToContextMenuFor(LP<P, Property<P>> mainProperty, String contextMenuCaption) {
        setAsEditActionFor(property.getSID(), mainProperty);
        mainProperty.property.setContextMenuAction(property.getSID(), contextMenuCaption);
    }

    public <P extends PropertyInterface> void setAsEditActionFor(String actionSID, LP<P, Property<P>> mainProperty) {
        assert listInterfaces.size() <= mainProperty.listInterfaces.size();

        //мэпим входы по порядку, у этого экшна входов может быть меньше
        ActionPropertyMapImplement<T, P> actionImplement = new ActionPropertyMapImplement<T, P>(property, getRevMap(mainProperty.listInterfaces));

        mainProperty.property.setEditAction(actionSID, actionImplement);
    }

    public void addOperand(boolean hasWhen, Object... params) {
        ImList<PropertyInterfaceImplement<T>> readImplements = PropertyUtils.readImplements(listInterfaces, params);
        ActionPropertyMapImplement<?, PropertyInterface> actImpl = (ActionPropertyMapImplement<?, PropertyInterface>)readImplements.get(0);
        if (property instanceof ListActionProperty) {
            ((ListActionProperty) property).addAction(actImpl);
        } else if (hasWhen) {
            ((CaseActionProperty) property).addCase((CalcPropertyMapImplement<?, PropertyInterface>)readImplements.get(1), actImpl);
        } else {
            ((CaseActionProperty) property).addOperand(actImpl);
        }
    }
}
