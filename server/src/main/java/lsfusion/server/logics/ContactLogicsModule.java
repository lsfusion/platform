package lsfusion.server.logics;

import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;


public class ContactLogicsModule extends ScriptingLogicsModule{

    public LCP firstNameContact;
    public LCP lastNameContact;
    public LCP nameContact;
    public LCP emailContact;
    public LCP contactEmail;

    public ContactLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(ContactLogicsModule.class.getResourceAsStream("/lsfusion/system/Contact.lsf"), "/lsfusion/system/Contact.lsf", baseLM, BL);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        firstNameContact = findProperty("firstName[Contact]");
        lastNameContact = findProperty("lastName[Contact]");
        nameContact = findProperty("name[Contact]");

        emailContact = findProperty("email[Contact]");
        contactEmail = findProperty("contact[VARSTRING[400]]");
    }
}
