package platform.server.view.form;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.logics.properties.Property;
import platform.server.session.TableChanges;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PropertyValueLink extends ValueLink {

    public PropertyValueLink(PropertyObjectImplement iProperty) {
        property = iProperty;
    }

    public PropertyValueLink(DataInputStream inStream, RemoteForm form, Type DBType) throws IOException {
        super(inStream, form, DBType);
        property = form.getPropertyView(inStream.readInt()).view;
    }

    public PropertyObjectImplement property;

/*    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        return property.getValueClass(ClassGroup);
    }*/

    @Override
    boolean classUpdated(GroupObjectImplement ClassGroup) {
        return property.classUpdated(ClassGroup);
    }

    @Override
    boolean objectUpdated(GroupObjectImplement ClassGroup) {
        return property.objectUpdated(ClassGroup);
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableChanges session, Type DBType, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        return property.getSourceExpr(classGroup, classSource, session, defaultProps, noUpdateProps);
    }
}
