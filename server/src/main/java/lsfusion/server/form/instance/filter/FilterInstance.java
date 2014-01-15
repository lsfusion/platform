package lsfusion.server.form.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.FilterType;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyValueImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.Modifier;

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

    public static FilterInstance deserialize(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
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

    public abstract Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException;

    public void resolveAdd(ExecutionEnvironment env, CustomObjectInstance object, DataObject addObject) throws SQLException, SQLHandledException {
    }

    public <X extends PropertyInterface> Set<CalcPropertyValueImplement<?>> getResolveChangeProperties(CalcProperty<X> toChange) {
        return new HashSet<CalcPropertyValueImplement<?>>();
    }

}
