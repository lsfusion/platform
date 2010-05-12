package platform.server.view.form;

import platform.server.logics.action.ActionInterface;
import platform.server.logics.action.Action;
import platform.server.logics.property.Property;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.sql.SQLException;

public class ActionObjectImplement extends ControlObjectImplement<ActionInterface, Action> {

    public ActionObjectImplement(Action property, Map<ActionInterface, ? extends PropertyObjectInterface> mapping) {
        super(property, mapping);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return false;
    }

    public void fillProperties(Set<Property> properties) {
    }

    // пока так но потом может по другому будет, надо будет классы проверять - (если например объектное в колонку будут записывать)
    public Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource, Modifier<? extends Changes> modifier) throws SQLException {
        return ValueExpr.TRUE;
    }
}
