package platform.server.view.form;

import java.util.Set;
import java.util.Map;
import java.io.DataInputStream;
import java.io.IOException;

import platform.server.logics.session.DataSession;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.data.types.Type;
import platform.server.data.query.exprs.SourceExpr;

public class PropertyValueLink extends ValueLink {

    public PropertyValueLink(PropertyObjectImplement iProperty) {
        property = iProperty;
    }

    public PropertyValueLink(DataInputStream inStream, RemoteForm form) throws IOException {
        super(inStream, form);
        property = form.getPropertyView(inStream.readInt()).view;
    }

    public PropertyObjectImplement property;

    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        return property.getValueClass(ClassGroup);
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return property.classUpdated(ClassGroup);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return property.objectUpdated(ClassGroup);
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return property.getSourceExpr(ClassGroup,ClassSource,Session);
    }
}
