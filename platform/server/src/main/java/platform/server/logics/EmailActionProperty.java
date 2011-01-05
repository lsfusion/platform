package platform.server.logics;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.interop.action.ClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.EmailSender;
import platform.server.auth.PolicyManager;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.reportstmp.ReportGenerator_tmp;
import platform.server.logics.property.*;

import java.io.File;
import java.util.*;
import java.sql.SQLException;

/**
 * User: DAle
 * Date: 03.01.11
 * Time: 11:53
 */

public class EmailActionProperty extends ActionProperty {
    private final List<FormEntity> forms;
    private final List<Map<ObjectEntity, ClassPropertyInterface>> mapObjects = new ArrayList<Map<ObjectEntity, ClassPropertyInterface>>();

    private final List<PropertyMapImplement<?, ClassPropertyInterface>> recepients = new ArrayList<PropertyMapImplement<?, ClassPropertyInterface>>();

    private final String subject;

    private final BusinessLogics<?> BL; // для возможности работы с формами в автоматическом режиме

    public EmailActionProperty(String sID, String caption, String mailSubject, BusinessLogics<?> BL, ValueClass[] classes) {
        super(sID, caption, classes);

        this.subject = mailSubject;
        forms = new ArrayList<FormEntity>();
        this.BL = BL;

        askConfirm = true;
    }

    public <R extends PropertyInterface> void addRecepient(PropertyMapImplement<R, ClassPropertyInterface> recepient) {
        recepients.add(recepient);
    }

    public void addForm(FormEntity form, Map<ObjectEntity, ClassPropertyInterface> objects) {
        forms.add(form);
        mapObjects.add(objects);
    }


    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) {
        throw new RuntimeException("should not be");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) throws SQLException {

        try {
            String embeddedFilePath = "";
            String[] reportPaths = new String[forms.size()-1];
            Map<ByteArray, String> files = new HashMap<ByteArray, String>();
            for (int i = 0; i < forms.size(); i++) {
                Map<ObjectEntity, DataObject> formObjects = BaseUtils.join(mapObjects.get(i), keys);
                RemoteFormInterface remoteForm;
                if(executeForm!=null)
                    remoteForm = executeForm.createForm(forms.get(i), formObjects);
                else
                    remoteForm = BL.createForm(session, forms.get(i), formObjects);

                ReportGenerator_tmp report = new ReportGenerator_tmp(remoteForm, true, files);
                JasperPrint print = report.createReport();
                print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

                File file = File.createTempFile("lsfReport", ".html");
                if (i == 0) {
                    embeddedFilePath = file.getAbsolutePath();
                } else {
                    reportPaths[i-1] = file.getAbsolutePath();
                }

                JRHtmlExporter htmlExporter = new JRHtmlExporter();
                htmlExporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, false);
                htmlExporter.setParameter(JRHtmlExporterParameter.OUTPUT_FILE_NAME, file.getAbsolutePath());
                htmlExporter.setParameter(JRHtmlExporterParameter.JASPER_PRINT, print);
                htmlExporter.exportReport();
            }

            Modifier<?> modifier = executeForm!=null? executeForm.form :session.modifier;

            List<String> recepientEmails = new ArrayList<String>();
            for(PropertyMapImplement<?, ClassPropertyInterface> recepient : recepients) {
                String recepientEmail = (String) recepient.read(session, keys, modifier);
                if(recepientEmail!=null)
                    recepientEmails.add(recepientEmail);
            }

            String smtpHost = (String) BL.smtpHost.read(session.sql, modifier, session.env);
            String fromAddress = (String) BL.fromAddress.read(session.sql, modifier, session.env);
            if(smtpHost==null || fromAddress==null)
                actions.add(new MessageClientAction("Не задан SMTP хост или адрес отправителя. Письма отосланы не будут.","Отсылка писем"));
            else {
                EmailSender sender = new EmailSender(smtpHost.trim(), fromAddress.trim(), recepientEmails.toArray(new String[recepientEmails.size()]));
                sender.sendMail(subject, embeddedFilePath, files, reportPaths);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
