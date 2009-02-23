package platform.server.view.form;

import java.util.Set;
import java.util.Map;

import platform.server.logics.session.DataSession;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.data.types.Type;
import platform.server.data.query.exprs.SourceExpr;

abstract public class ValueLink {

    ClassSet getValueClass(GroupObjectImplement ClassGroup) {return null;}

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {return false;}

    boolean ObjectUpdated(GroupObjectImplement ClassGroup) {return false;}

    public abstract SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType);
}
