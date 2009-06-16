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

public class ObjectValueLink extends ValueLink {

    public ObjectValueLink(DataInputStream inStream, RemoteForm form, Type DBType) throws IOException {
        super(inStream, form, DBType);
        object = form.getObjectImplement(inStream.readInt());
    }

    public ObjectValueLink(ObjectImplement iObject) {
        object =iObject;
    }

    public ObjectImplement object;

/*    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        if(object.customClass ==null)
            return new ClassSet();
        else
            return new ClassSet(object.customClass);
    }*/

    @Override
    boolean classUpdated(GroupObjectImplement classGroup) {
        return object.classUpdated();
    }

    @Override
    boolean objectUpdated(GroupObjectImplement classGroup) {
        return object.objectUpdated();
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableChanges session, Type DBType, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        return object.getSourceExpr(classGroup, classSource);
    }
}
