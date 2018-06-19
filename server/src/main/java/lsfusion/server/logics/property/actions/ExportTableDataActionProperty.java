package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.data.JDBCTable;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;

import java.io.IOException;

public class ExportTableDataActionProperty<I extends PropertyInterface> extends ExportDataActionProperty<I> {
    private boolean singleRow;
    public ExportTableDataActionProperty(LocalizedString caption, String extension, ImSet<I> innerInterfaces, ImOrderSet<I> mapInterfaces,
                                         ImOrderSet<String> fields, ImMap<String, CalcPropertyInterfaceImplement<I>> exprs, ImMap<String, Type> types,
                                         CalcPropertyInterfaceImplement<I> where, ImOrderMap<String, Boolean> orders, LCP targetProp, boolean singleRow) {
        super(caption, extension, innerInterfaces, mapInterfaces, fields, exprs, types, where, orders, targetProp);
        this.singleRow = singleRow;
    }

    @Override
    protected byte[] getFile(Query<I, String> query, ImList<ImMap<String, Object>> rows, Type.Getter<String> fieldTypes) throws IOException {
        return JDBCTable.serialize(singleRow, fields, fieldTypes, rows);
    }
}