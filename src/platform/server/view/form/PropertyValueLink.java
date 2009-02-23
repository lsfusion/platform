package platform.server.view.form;

import java.util.Set;
import java.util.Map;

import platform.server.logics.session.DataSession;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.data.types.Type;
import platform.server.data.query.exprs.SourceExpr;

public class PropertyValueLink extends ValueLink {

    public PropertyValueLink(PropertyObjectImplement iProperty) {Property=iProperty;}

    public PropertyObjectImplement Property;

    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        return Property.getValueClass(ClassGroup);
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return Property.classUpdated(ClassGroup);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return Property.objectUpdated(ClassGroup);
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return Property.getSourceExpr(ClassGroup,ClassSource,Session);
    }
}
