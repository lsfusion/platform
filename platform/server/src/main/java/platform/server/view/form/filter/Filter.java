package platform.server.view.form.filter;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.logics.properties.Property;
import platform.server.session.TableChanges;
import platform.server.view.form.RemoteForm;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.where.Where;
import platform.interop.FilterType;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Filter {

    public Filter() {
    }

    public Filter(DataInputStream inStream, RemoteForm form) throws IOException {
    }

    public static Filter deserialize(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case FilterType.OR:
                return new OrFilter(inStream, form);
            case FilterType.COMPARE:
                return new CompareFilter(inStream, form);
            case FilterType.NOTNULL:
                return new NotNullFilter(inStream, form);
            case FilterType.ISCLASS:
                return new IsClassFilter(inStream, form);
        }

        throw new IOException();
    }

    public abstract GroupObjectImplement getApplyObject();

    public boolean isInInterface(GroupObjectImplement classGroup) {
        return true; // пока будем считать что нету в интерфейсе и включать тоже не будем
        /* AndClassSet valueClass = value.getValueClass(classGroup);
        if(valueClass==null)
            return property.isInInterface(classGroup);
        else
            return property.getValueClass(classGroup).intersect(valueClass); */
    }

    public abstract boolean classUpdated(GroupObjectImplement classGroup);

    public abstract boolean objectUpdated(GroupObjectImplement classGroup);

    public abstract boolean dataUpdated(Collection<Property> changedProps);

    public abstract Where getWhere(Map<ObjectImplement, KeyExpr> mapKeys, Set<GroupObjectImplement> classGroup, TableChanges session, Property.TableDepends<? extends Property.TableUsedChanges> depends) throws SQLException;

    protected abstract void fillProperties(Set<Property> properties);

    public Set<Property> getProperties() {
        Set<Property> properties = new HashSet<Property>();
        fillProperties(properties);
        return properties;
    }
}
