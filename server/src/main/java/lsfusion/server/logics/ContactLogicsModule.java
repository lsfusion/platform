package lsfusion.server.logics;

import org.antlr.runtime.RecognitionException;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;


public class ContactLogicsModule extends ScriptingLogicsModule{

    public LCP firstNameContact;
    public LCP lastNameContact;
    public LCP nameContact;
    public LCP emailContact;
    public LCP contactEmail;

    public ContactLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ContactLogicsModule.class.getResourceAsStream("/scripts/system/Contact.lsf"), baseLM, BL);
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        firstNameContact = getLCPByOldName("firstNameContact");
        lastNameContact = getLCPByOldName("lastNameContact");
        nameContact = getLCPByOldName("nameContact");

        emailContact = getLCPByOldName("emailContact");
        emailContact.setRegexp("^[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+(?:\\.[-a-zA-Z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-zA-Z0-9]([-a-zA-Z0-9]{0,61}[a-zA-Z0-9])?\\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-zA-Z][a-zA-Z])$");
        emailContact.setRegexpMessage("<html>Неверный формат e-mail</html>");
        contactEmail = getLCPByOldName("contactEmail");
    }
}
