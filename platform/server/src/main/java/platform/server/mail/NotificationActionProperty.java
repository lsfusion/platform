package platform.server.mail;

import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.CalcPropertyMapImplement;
import platform.server.logics.property.actions.SystemActionProperty;

import javax.mail.Message;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationActionProperty extends SystemActionProperty {
    private final static Logger logger = Logger.getLogger(NotificationActionProperty.class);

    private final LinkedHashMap<CalcPropertyMapImplement<?, ClassPropertyInterface>, Message.RecipientType> recipients = new LinkedHashMap<CalcPropertyMapImplement<?, ClassPropertyInterface>, Message.RecipientType>();

    private final String subjectNotification;
    private final String textNotification;
    private final String emailFromNotification;
    private final String emailToNotification;
    private final String emailToCCNotification;
    private final String emailToBCNotification;

    private final BusinessLogics<?> BL;

    @Override
    public PropsNewSession aspectUsedExtProps() {
        return getUsedProps(recipients.keySet());
    }

    public NotificationActionProperty(String sID, String caption, LCP targetProperty, String subjectNotification, String textNotification, String emailFromNotification, String emailToNotification, String emailToCCNotification, String emailToBCNotification, BusinessLogics<?> BL) {
        super(sID, caption, getValueClasses(targetProperty));

        this.subjectNotification = subjectNotification;
        this.textNotification = textNotification;
        this.emailFromNotification = emailFromNotification;
        this.emailToNotification = emailToNotification;
        this.emailToCCNotification = emailToCCNotification;
        this.emailToBCNotification = emailToBCNotification;
        this.BL = BL;

        askConfirm = true;
        setImage("email.png");
    }

    private static ValueClass[] getValueClasses(LCP sourceProperty) {
        return sourceProperty.getInterfaceClasses();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        if (BL.LM.disableEmail.read(context) != null) {
            logger.error(ServerResourceBundle.getString("mail.sending.disabled"));
            return;
        }

        Map<String, Message.RecipientType> recipientEmails = new HashMap<String, Message.RecipientType>();

        if (emailToNotification != null)
            for (String email : emailToNotification.split(";")) {
                if (email != null && (!recipientEmails.containsKey(email.trim())))
                    recipientEmails.put(email.trim(), Message.RecipientType.TO);
            }
        if (emailToCCNotification != null)
            for (String email : emailToCCNotification.split(";")) {
                if (email != null && (!recipientEmails.containsKey(email.trim())))
                    recipientEmails.put(email.trim(), Message.RecipientType.CC);
            }
        if (emailToBCNotification != null)
            for (String email : emailToBCNotification.split(";")) {
                if (email != null && (!recipientEmails.containsKey(email.trim())))
                    recipientEmails.put(email.trim(), Message.RecipientType.BCC);
            }

        Pattern p = Pattern.compile("\\{(.*?)\\((.*?)\\)\\}");
        Matcher m = p.matcher(textNotification);
        String currentText = textNotification;
        while (m.find()) {
            String propertySID = m.group(1);
            int interfacesCount = m.group(2).split(",").length;
            LCP replaceProperty = (LCP) BL.LM.getLP(propertySID.trim());
            Object replacePropertyValue;
            if (!"".equals(m.group(2))) {
                DataObject[] objects = new DataObject[interfacesCount];
                for (int i = 0; i < interfacesCount; i++) {
                    for (int j = 0; j < context.getKeyCount(); j++) {
                        String sidFromReplaceProperty = getValueClasses(replaceProperty)[i].getSID();
                        String sidFromContext = ((ClassPropertyInterface) (context.getKeys().keySet().toArray()[j])).interfaceClass.getSID();
                        if (sidFromReplaceProperty.equals(sidFromContext))
                            objects[i] = (DataObject) context.getKeys().values().toArray()[j];
                    }
                }
                replacePropertyValue = replaceProperty.read(context, objects);
            } else
                replacePropertyValue = replaceProperty.read(context);
            if (replacePropertyValue != null)
                currentText = currentText.replace("$P{" + replaceProperty.property.getSID() + "(" + m.group(2) + ")}", replacePropertyValue.toString().trim());
        }

        List<EmailSender.AttachmentProperties> attachmentForms = new ArrayList<EmailSender.AttachmentProperties>();
        Map<ByteArray, String> attachmentFiles = new HashMap<ByteArray, String>();

        String encryptedConnectionType = (String) BL.LM.nameEncryptedConnectionType.read(context);
        String smtpHost = (String) BL.LM.smtpHost.read(context);
        String smtpPort = (String) BL.LM.smtpPort.read(context);
        String userName = (String) BL.LM.emailAccount.read(context);
        String password = (String) BL.LM.emailPassword.read(context);

        if (smtpHost == null || emailFromNotification == null) {
            String errorMessage = ServerResourceBundle.getString("mail.smtp.host.or.sender.not.specified.letters.will.not.be.sent");
            logger.error(errorMessage);
            context.delayUserInterfaction(new MessageClientAction(errorMessage, ServerResourceBundle.getString("mail.sending")));
        } else {
            EmailSender sender = new EmailSender(smtpHost.trim(),BaseUtils.nullTrim(smtpPort), encryptedConnectionType.trim(), emailFromNotification.trim(), BaseUtils.nullTrim(userName), BaseUtils.nullTrim(password), recipientEmails);
            try {
                sender.sendPlainMail(subjectNotification, currentText, attachmentForms, attachmentFiles);
            } catch (Exception e) {
                String errorMessage = ServerResourceBundle.getString("mail.failed.to.send.mail") + " : " + e.toString();
                logger.error(errorMessage);
                context.delayUserInterfaction(new MessageClientAction(errorMessage, ServerResourceBundle.getString("mail.sending")));
                e.printStackTrace();
            }
        }

    }
}
