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

    public TimePropertyChangeListener(CalcProperty<P> property, Time time, DataProperty timeProperty, List<P> propInterfaces) {
        super(property, new CalcPropertyMapImplement<ClassPropertyInterface, P>(timeProperty, timeProperty.getMapInterfaces(propInterfaces)));

        this.time = time;
        this.timeProperty = timeProperty;
    }

    @Override
    protected Expr getValueExpr() {
        return new TimeExpr(time);
    }

}
