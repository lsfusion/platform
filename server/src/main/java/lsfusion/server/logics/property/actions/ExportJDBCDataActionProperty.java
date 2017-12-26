package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;

import java.io.IOException;

public class ExportJDBCDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {

    public ExportJDBCDataActionProperty(LocalizedString caption,
                                        ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                        ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, CalcPropertyInterfaceImplement<I> where, LCP targetProp) {
        super(caption, innerInterfaces, mapInterfaces, fields, exprs, where, targetProp);
    }

    @Override
    protected byte[] getExtension() {
        return "jdbc".getBytes();
    }

    @Override
    protected byte[] getFile(Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        return JDBCTable.serialize(fields, fieldTypes, rows);
    }
}