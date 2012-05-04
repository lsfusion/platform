package platform.server.logics.property.change;

import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.data.Time;
import platform.server.data.expr.Expr;
import platform.server.data.expr.TimeExpr;
import platform.server.logics.property.*;

import java.util.List;

public class TimePropertyChangeListener<P extends PropertyInterface> extends PropertyChangeListener<P> {
    private final Time time;
    public final DataProperty timeProperty;

    public TimePropertyChangeListener(Property<P> property, boolean isStored, Time time, String sID, String caption, ValueClass[] classes, List<P> propInterfaces) {
        this(property, time, createTimeProperty(isStored, time, sID, caption, classes), propInterfaces);
    }

    private TimePropertyChangeListener(Property<P> property, Time time, DataProperty timeProperty, List<P> propInterfaces) {
        super(property, new PropertyImplement<ClassPropertyInterface, P>(timeProperty, timeProperty.getMapInterfaces(propInterfaces)));

        this.time = time;
        this.timeProperty = timeProperty;
    }

    @Override
    protected Expr getValueExpr() {
        return new TimeExpr(time);
    }

    private static DataProperty createTimeProperty(boolean isStored, Time time, String sID, String caption, ValueClass[] classes) {
        ConcreteValueClass valueClass = time.getConcreteValueClass();
        return isStored
               ? new StoredDataProperty(sID, caption, classes, valueClass)
               : new SessionDataProperty(sID, caption, classes, valueClass);
    }
}
