package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

// вообщем то нужен когда в Object... есть одновременно и calc и action'ы (в If, For и т.п. см. readImplements)
public interface PropertyInterfaceImplement<P extends PropertyInterface> {
}
