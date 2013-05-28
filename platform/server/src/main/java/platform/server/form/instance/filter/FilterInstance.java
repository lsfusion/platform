package platform.server.form.instance.filter;

import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.FilterType;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyValueImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public abstract class FilterInstance implements Updated {

    // даже если не в интерфейсе все равно ставить (то есть по сути фильтр делать false)
    public final static boolean ignoreInInterface = true;
    public boolean junction; //true - conjunction, false - disjunction

    public FilterInstance() {
    }

    public FilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
    }

    public static FilterInstance deserialize(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case FilterType.OR:
                return new OrFilterInstance(inStream, form);
            case FilterType.AND:
                return new AndFilterInstance(inStream, form);
            case FilterType.COMPARE:
                CompareFilterInstance filter = new CompareFilterInstance(inStream, form);
                if (filter.value instanceof NullValue) {
                    FilterInstance notNullFilter = new NotNullFilterInstance(filter.property);
                    notNullFilter.junction = filter.junction;
                    if (!filter.negate) {
                        NotFilterInstance notFilter = new NotFilterInstance(notNullFilter);
                        notFilter.junction = notNullFilter.junction;
                        return notFilter;
                    } else {
                        return notNullFilter;
                    }
                }
                else
                    return filter;
            case FilterType.NOTNULL:
                return new NotNullFilterInstance(inStream, form);
            case FilterType.ISCLASS:
                return new IsClassFilterInstance(inStream, form);
            case FilterType.NOT:
                return new NotFilterInstance(inStream, form);
        }

        throw new IOException();
    }

    public abstract GroupObjectInstance getApplyObject();

    public abstract Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier);

    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject) throws SQLException {
    }

    public <X extends PropertyInterface> Set<CalcPropertyValueImplement<?>> getResolveChangeProperties(CalcProperty<X> toChange) {
        return new HashSet<CalcPropertyValueImplement<?>>();
    }

}
