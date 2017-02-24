package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.FormExportType;
import lsfusion.interop.FormStaticType;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcPropertyMapImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.exporting.HierarchicalFormExporter;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;
import lsfusion.server.logics.property.actions.exporting.csv.CSVFormExporter;
import lsfusion.server.logics.property.actions.exporting.dbf.DBFFormExporter;
import lsfusion.server.logics.property.actions.exporting.json.JSONFormExporter;
import lsfusion.server.logics.property.actions.exporting.xml.XMLFormExporter;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class ExportActionProperty extends FormStaticActionProperty<FormExportType> {

    // csv
    private final boolean noHeader;
    private final String separator;
    private final String charset;

    public ExportActionProperty(LocalizedString caption,
                                    FormEntity form,
                                    final ObjectEntity[] objectsToSet,
                                    boolean allowNulls, 
                                    FormExportType staticType,
                                    LCP formExportFile,
                                    boolean noHeader,
                                    String separator,
                                    String charset) {
        super(caption, form, objectsToSet, allowNulls, staticType, formExportFile);
        
        this.noHeader = noHeader;
        this.separator = separator;
        this.charset = charset;
    }


    @Override
    protected Map<String, byte[]> exportPlain(ReportGenerationData reportData) throws IOException {
        PlainFormExporter exporter;
        if(staticType == FormExportType.CSV) {
            exporter = new CSVFormExporter(reportData, noHeader, separator, charset);
        } else {
            assert staticType == FormExportType.DBF;
            exporter = new DBFFormExporter(reportData, charset);
        }
        return exporter.export();
    }

    @Override
    protected byte[] exportHierarchical(ReportGenerationData reportData) throws JRException, IOException, ClassNotFoundException {
        HierarchicalFormExporter exporter;
        if (staticType == FormExportType.XML) {
            exporter = new XMLFormExporter(reportData);
        } else {
            assert staticType == FormExportType.JSON;
            exporter = new JSONFormExporter(reportData);
        }
        return exporter.export();
    }

    @Override
    protected void exportClient(ExecutionContext<ClassPropertyInterface> context, ReportGenerationData reportData, Map<String, String> reportPath) throws SQLException, SQLHandledException {
        throw new UnsupportedOperationException();
    }
}
