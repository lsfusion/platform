package platform.server.view.form;

import java.util.Set;
import java.util.Map;

import platform.server.logics.session.DataSession;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.data.types.Type;
import platform.server.data.query.exprs.SourceExpr;

public class ObjectValueLink extends ValueLink {

    public ObjectValueLink(ObjectImplement iObject) {Object=iObject;}

    public ObjectImplement Object;

    @Override
    ClassSet getValueClass(GroupObjectImplement ClassGroup) {
        if(Object.Class==null)
            return new ClassSet();
        else
            return new ClassSet(Object.Class);
    }

    @Override
    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.updated & ObjectImplement.UPDATED_CLASS)!=0);
    }

    @Override
    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {
        return ((Object.updated & ObjectImplement.UPDATED_OBJECT)!=0);
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return Object.getSourceExpr(ClassGroup,ClassSource);
    }
}
