package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.where.WhereBuilder;

import java.util.Collection;
import java.util.Map;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere);

    abstract void mapFillDepends(Collection<Property> depends);

    MapChangeDataProperty<P> mapGetChangeProperty(Map<P, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID);
}
