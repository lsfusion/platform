package platform.server.logics;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.interop.action.ClientAction;
import platform.interop.form.RemoteFormInterface;
import platform.server.EmailSender;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.reportstmp.ReportGenerator_tmp;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;

import java.io.File;
import java.util.*;

/**
 * User: DAle
 * Date: 03.01.11
 * Time: 11:53
 */

public class EmailActionProperty extends ActionProperty {
    private final List<FormEntity> forms;
    private final List<Map<ObjectEntity, ClassPropertyInterface>> mapObjects = new ArrayList<Map<ObjectEntity, ClassPropertyInterface>>();
    private final String subject;

    public static ValueClass[] getValueClasses(List<List<ObjectEntity>> objects) {
        List<ValueClass> valueClasses = new ArrayList<ValueClass>();
        for(List<ObjectEntity> formObjects : objects) {
            for (ObjectEntity object : formObjects) {
                valueClasses.add(object.baseClass);
            }
        }
        return valueClasses.toArray(new ValueClass[valueClasses.size()]);
    }

    public EmailActionProperty(String sID, String caption, String mailSubject, List<FormEntity> forms, List<List<ObjectEntity>> objects) {
        super(sID, caption, getValueClasses(objects));
        this.subject = mailSubject;
        this.forms = forms;
        Iterator<ClassPropertyInterface> iter = interfaces.iterator();
        for (List<ObjectEntity> formObjects : objects) {
            Map<ObjectEntity, ClassPropertyInterface> mapping = new HashMap<ObjectEntity, ClassPropertyInterface>();
            for (ObjectEntity object : formObjects) {
                mapping.put(object, iter.next());
            }
            mapObjects.add(mapping);
        }
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapExecuteObjects) {
        String embeddedFilePath = "";
        String[] reportPaths = new String[forms.size()-1];
        Map<ByteArray, String> files = new HashMap<ByteArray, String>();
        for (int i = 0; i < forms.size(); i++) {
            Map<ObjectEntity, DataObject> mapping = new HashMap<ObjectEntity, DataObject>();
            if (mapObjects != null && mapObjects.size() > i && mapObjects.get(i) != null) {
                mapping = BaseUtils.join(mapObjects.get(i), keys);
            }
            RemoteFormInterface remoteForm = executeForm.createForm(forms.get(i), mapping);

            try {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        EmailSender sender = new EmailSender("danchenko@gmail.com", "dale@luxsoft.by");
        sender.sendMail(subject, embeddedFilePath, files, reportPaths);
    }

}
