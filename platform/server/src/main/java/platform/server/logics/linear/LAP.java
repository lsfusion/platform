package platform.server.logics.linear;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.interop.form.ServerResponse;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.logics.property.actions.flow.Inline;
import platform.server.session.DataSession;

import java.sql.SQLException;

import static platform.server.logics.PropertyUtils.readCalcImplements;

public class LAP<T extends PropertyInterface> extends LP<T, ActionProperty<T>> {

    public LAP(ActionProperty<T> property) {
        super(property);
    }

    public LAP(ActionProperty<T> property, ImOrderSet<T> listInterfaces) {
        super(property, listInterfaces);
    }

    public void execute(DataSession session, DataObject... objects) throws SQLException {
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
        return listInterfaces.mapOrder(property.getInterfaceClasses()).toArray(new ValueClass[listInterfaces.size()]);
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
}
