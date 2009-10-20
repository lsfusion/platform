package platform.server.view.form.filter;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.Updated;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public interface CompareValue extends Updated {

//    AndClassSet getValueClass(GroupObjectImplement ClassGroup) {return null;}

    SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableModifier<? extends TableChanges> modifier) throws SQLException;
}
