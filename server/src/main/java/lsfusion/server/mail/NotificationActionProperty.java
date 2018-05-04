package lsfusion.server.mail;

import lsfusion.base.ByteArray;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.EmailLogicsModule;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;
import org.apache.log4j.Logger;

import javax.mail.Message;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.nullTrim;
import static lsfusion.base.BaseUtils.trim;
import static lsfusion.server.context.ThreadLocalContext.localize;

public class NotificationActionProperty extends SystemExplicitActionProperty {
    private final static Logger logger = ServerLoggers.mailLogger;

    private final String subjectNotification;
    private final String textNotification;
    private final String emailFromNotification;
    private final String emailToNotification;
    private final String emailToCCNotification;
    private final String emailToBCNotification;

    private final EmailLogicsModule emailLM;

    public NotificationActionProperty(LocalizedString caption, LCP targetProperty, String subjectNotification, String textNotification, String emailFromNotification, String emailToNotification, String emailToCCNotification, String emailToBCNotification, EmailLogicsModule emailLM) {
        super(caption, getValueClasses(targetProperty));

        this.subjectNotification = subjectNotification;
        this.textNotification = textNotification;
        this.emailFromNotification = emailFromNotification;
        this.emailToNotification = emailToNotification;
        this.emailToCCNotification = emailToCCNotification;
        this.emailToBCNotification = emailToBCNotification;

        this.emailLM = emailLM;

        drawOptions.setAskConfirm(true);
        drawOptions.setImage("email.png");
    }

    private static ValueClass[] getValueClasses(LCP sourceProperty) {
        return sourceProperty.getInterfaceClasses(ClassType.logPolicy);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ObjectValue defaultAccount = emailLM.defaultNotificationAccount.readClasses(context.getSession());

        if (defaultAccount instanceof DataObject) {

            if (emailLM.disableAccount.read(context, defaultAccount) == null) {

                Map<String, Message.RecipientType> recipientEmails = new HashMap<>();

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

                // todo [dale]: Теперь передаваться будет не sid свойства, а каноническое имя, которое может содержать символы: '.' ',' '{' '}' '[' ']' '(' ')'
                // Поэтому код ниже надо как-то менять
                Pattern p = Pattern.compile("\\{(.*?)\\((.*?)\\)\\}");
                Matcher m = p.matcher(textNotification);
                String currentText = textNotification;
                while (m.find()) {
                    String propertyCanonicalName = m.group(1);
                    int interfacesCount = m.group(2).split(",").length;
                    LCP replaceProperty = null;
                    try {
                        replaceProperty = (LCP) context.getBL().findProperty(propertyCanonicalName.trim());
                    } catch (Exception ignored) {
                    }
                    if (replaceProperty != null) {
                        Object replacePropertyValue = null;
                        if (!m.group(2).isEmpty()) {
                            List<ObjectValue> objects = new ArrayList<>();
                            for (int i = 0; i < interfacesCount; i++) {
                                for (int j = 0; j < context.getKeyCount(); j++) {
                                    ValueClass classFromReplaceProperty = getValueClasses(replaceProperty)[i];
                                    ValueClass classFromContext = context.getKeys().getKey(j).interfaceClass;
                                    if (classFromReplaceProperty.isCompatibleParent(classFromContext)) {
                                        objects.add(context.getKeys().getValue(j));
                                        break;
                                    }
                                }
                            }
                            if(objects.size() == interfacesCount)
                                replacePropertyValue = replaceProperty.read(context, objects.toArray(new ObjectValue[objects.size()]));
                        } else
                            replacePropertyValue = replaceProperty.read(context);
                        if (replacePropertyValue != null)
                            currentText = currentText.replace(m.group(0), trim((String) replacePropertyValue));
                    }
                }

                List<EmailSender.AttachmentProperties> attachmentForms = new ArrayList<>();
                Map<ByteArray, String> attachmentFiles = new HashMap<>();

                String encryptedConnectionType = (String) emailLM.nameEncryptedConnectionTypeAccount.read(context, defaultAccount);
                String smtpHostAccount = (String) emailLM.smtpHostAccount.read(context, defaultAccount);
                String smtpPortAccount = (String) emailLM.smtpPortAccount.read(context, defaultAccount);
                String nameAccount = (String) emailLM.nameAccount.read(context, defaultAccount);
                String password = (String) emailLM.passwordAccount.read(context, defaultAccount);

                if (smtpHostAccount == null || emailFromNotification == null) {
                    String errorMessage = localize("{mail.smtp.host.or.sender.not.specified.letters.will.not.be.sent}");
                    logger.error(errorMessage);
                    context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.sending}")));
                } else {
                    EmailSender sender = new EmailSender(nullTrim(smtpHostAccount), nullTrim(smtpPortAccount), nullTrim(encryptedConnectionType), nullTrim(emailFromNotification), nullTrim(nameAccount), nullTrim(password), recipientEmails);
                    try {
                        sender.sendPlainMail(context, subjectNotification, currentText, attachmentForms, attachmentFiles);
                    } catch (Exception e) {
                        String errorMessage = localize("{mail.failed.to.send.mail}") + " : " + e.toString();
                        logger.error(errorMessage);
                        context.delayUserInterfaction(new MessageClientAction(errorMessage, localize("{mail.sending}")));
                        e.printStackTrace();
                    }
                }
            }
        } else {
            logger.error(localize("{mail.disabled}"));
        }
    }
}
