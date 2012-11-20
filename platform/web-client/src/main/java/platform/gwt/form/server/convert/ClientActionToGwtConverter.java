package platform.gwt.form.server.convert;

import com.google.common.base.Throwables;
import jasperapi.ReportGenerator;
import net.sf.jasperreports.engine.JasperExportManager;
import platform.base.BaseUtils;
import platform.client.logics.ClientFormChanges;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.gwt.base.server.LogicsDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.view.actions.*;
import platform.gwt.form.shared.view.changes.dto.GFormChangesDTO;
import platform.gwt.form.shared.view.classes.GObjectClass;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.window.GModalityType;
import platform.interop.ModalityType;
import platform.interop.action.*;
import platform.interop.form.ReportGenerationData;

import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.Calendar;
import java.util.TimeZone;

import static platform.base.BaseUtils.deserializeObject;

@SuppressWarnings("UnusedDeclaration")
public class ClientActionToGwtConverter extends ObjectConverter {
    private static final class InstanceHolder {
        private static final ClientActionToGwtConverter instance = new ClientActionToGwtConverter();
    }

    public static ClientActionToGwtConverter getInstance() {
        return InstanceHolder.instance;
    }

    private final ClientTypeToGwtConverter typeConverter = ClientTypeToGwtConverter.getInstance();
    private final ClientFormChangesToGwtConverter valuesConverter = ClientFormChangesToGwtConverter.getInstance();

    private ClientActionToGwtConverter() {
    }

    public GAction convertAction(ClientAction clientAction, Object... context) {
        return convertOrNull(clientAction, context);
    }

    @Converter(from = ChooseClassClientAction.class)
    public GChooseClassAction convertAction(ChooseClassClientAction action) throws IOException {
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(action.classes));

        GObjectClass baseClass = typeConverter.convertOrCast(
                (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream)
        );

        GObjectClass defaultClass = typeConverter.convertOrCast(
                (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inputStream)
        );

        return new GChooseClassAction(action.concrete, baseClass, defaultClass);
    }

    @Converter(from = ConfirmClientAction.class)
    public GConfirmAction convertAction(ConfirmClientAction action) {
        return new GConfirmAction(action.message, action.caption);
    }

    @Converter(from = DialogClientAction.class)
    public GDialogAction convertAction(DialogClientAction action, RemoteServiceImpl servlet) throws IOException {
        return new GDialogAction(servlet.getFormSessionManager().createForm(action.dialog));
    }

    @Converter(from = FormClientAction.class)
    public GFormAction convertAction(FormClientAction action, RemoteServiceImpl servlet) throws IOException {
        GModalityType modalityType = convertOrCast(action.modalityType);
        return new GFormAction(modalityType, servlet.getFormSessionManager().createForm(action.remoteForm));
    }

    @Converter(from = ModalityType.class)
    public GModalityType convertModalityType(ModalityType modalityType) {
        switch (modalityType) {
            case DOCKED: return GModalityType.DOCKED;
            case MODAL: return GModalityType.MODAL;
            case FULLSCREEN_MODAL: return GModalityType.FULLSCREEN_MODAL;
            case DOCKED_MODAL: return GModalityType.DOCKED_MODAL;
        }
        return null;
    }

    @Converter(from = HideFormClientAction.class)
    public GHideFormAction convertAction(HideFormClientAction action, LogicsDispatchServlet servlet) throws IOException {
        return new GHideFormAction();
    }

    @Converter(from = LogMessageClientAction.class)
    public GLogMessageAction convertAction(LogMessageClientAction action, LogicsDispatchServlet servlet) throws IOException {
        return new GLogMessageAction(action.failed, action.message, action.data, action.titles);
    }

    @Converter(from = MessageClientAction.class)
    public GMessageAction convertAction(MessageClientAction action) {
        return new GMessageAction(action.message, action.caption);
    }

    @Converter(from = ProcessFormChangesClientAction.class)
    public GProcessFormChangesAction convertAction(ProcessFormChangesClientAction action, FormSessionObject form) throws IOException {
        ClientFormChanges changes = new ClientFormChanges(new DataInputStream(new ByteArrayInputStream(action.formChanges)), form.clientForm);

        GFormChangesDTO changesDTO = ClientFormChangesToGwtConverter.getInstance().convertOrCast(changes);

        return new GProcessFormChangesAction(changesDTO);
    }

    @Converter(from = ReportClientAction.class)
    public GReportAction convertAction(ReportClientAction action, HttpSession session, FormSessionObject form) throws IOException {
        return new GReportAction(generateReport(session, false, action.generationData));
    }

    @Converter(from = RequestUserInputClientAction.class)
    public GRequestUserInputAction convertAction(RequestUserInputClientAction action) throws IOException {
        GType type = typeConverter.convertOrCast(
                ClientTypeSerializer.deserializeClientType(action.readType)
        ) ;

        Object value = deserializeServerValue(action.oldValue);

        return new GRequestUserInputAction(type, value);
    }

    private Object deserializeServerValue(byte[] valueBytes) throws IOException {
        return valuesConverter.convertOrCast(deserializeObject(valueBytes));
    }

    @Converter(from = RunPrintReportClientAction.class)
    public GRunPrintReportAction convertAction(RunPrintReportClientAction action, HttpSession session, FormSessionObject form) throws IOException {
        return new GRunPrintReportAction(generateReport(session, false, form.remoteForm.getReportData(-1, null, false, null)));
    }

    @Converter(from = RunOpenInExcelClientAction.class)
    public GRunOpenInExcelAction convertAction(RunOpenInExcelClientAction action, HttpSession session, FormSessionObject form) throws IOException {
        return new GRunOpenInExcelAction(generateReport(session, true, form.remoteForm.getReportData(-1, null, true, null)));
    }

    @Converter(from = AsyncResultClientAction.class)
    public GAsyncResultAction convertAction(AsyncResultClientAction action, HttpSession session, FormSessionObject form) throws IOException {
        return new GAsyncResultAction(deserializeServerValue(action.value));
    }

    private String generateReport(HttpSession session, boolean toExcel, ReportGenerationData reportData) {
        try {
            TimeZone zone = Calendar.getInstance().getTimeZone();
            ReportGenerator generator = new ReportGenerator(reportData, zone);
            byte[] report = !toExcel ? JasperExportManager.exportReportToPdf(generator.createReport(false, null)) : ReportGenerator.exportToExcelByteArray(reportData, zone);
            File file = File.createTempFile("lsfReport", "");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(report);

            String reportSID = generateReportSID(session);
            session.setAttribute(reportSID, file.getAbsolutePath());
            return reportSID;
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return null;
    }

    private String generateReportSID(HttpSession session) {
        String sid;
        do {
            sid = BaseUtils.randomString(20);
        } while (session.getAttribute(sid) != null);
        return sid;
    }
}
