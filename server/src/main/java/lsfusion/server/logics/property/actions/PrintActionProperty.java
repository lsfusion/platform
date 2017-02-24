package lsfusion.server.logics.property.actions;

import jasperapi.ReportGenerator;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.FormStaticType;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PrintActionProperty extends FormStaticActionProperty<FormPrintType> {

    private CalcPropertyInterfaceImplement<ClassPropertyInterface> printerProperty;

    private final LCP formPageCount;

    public PrintActionProperty(LocalizedString caption,
                                    FormEntity form,
                                    final ObjectEntity[] objectsToSet,
                                    boolean allowNulls,
                                    FormPrintType staticType,
                                    LCP formExportFile,
                                    CalcPropertyMapImplement printer,
                                    ImOrderSet<PropertyInterface> innerInterfaces,
                                    LCP formPageCount) {
        super(caption, form, objectsToSet, allowNulls, staticType, formExportFile, printer == null ? null : printer.property);

        this.formPageCount = formPageCount;

        if(printer != null) {
            ImRevMap<PropertyInterface, ClassPropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
            this.printerProperty = printer.map(mapInterfaces);
        }
    }

    @Override
    protected Map<String, byte[]> exportPlain(ReportGenerationData reportData) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte[] exportHierarchical(ReportGenerationData reportData) throws JRException, IOException, ClassNotFoundException {
        return IOUtils.getFileBytes(ReportGenerator.exportToFile(reportData, (FormPrintType) staticType));
    }

    @Override
    protected void exportClient(ExecutionContext<ClassPropertyInterface> context, ReportGenerationData reportData, Map<String, String> reportPath) throws SQLException, SQLHandledException {
        String pName = printerProperty == null ? null : (String) printerProperty.read(context, context.getKeys());
        Object pageCount = context.requestUserInteraction(new ReportClientAction(reportPath, false, reportData, (FormPrintType) staticType, pName, SystemProperties.isDebug));
        formPageCount.change(pageCount, context);
    }
}
