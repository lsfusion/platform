package platform.server.view.form.filter;

import platform.server.data.expr.Expr;
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

    Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource, TableModifier<? extends TableChanges> modifier) throws SQLException;
}
