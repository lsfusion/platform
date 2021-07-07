---
title: 'Access from an internal system'
---

### Java interaction

In this type of interaction, the internal system can directly access the Java elements of the lsFusion system (as with ordinary Java objects). This means you can perform all the same operations as when using network protocols, while avoiding the significant overhead of such interaction (such as serialization of parameters/deserialization of the result, etc.). In addition, this method of communication is much more convenient and efficient if the interaction is very close (i.e., the process of performing one operation requires constant access to both sides – from the lsFusion system to another system and vice versa) and/or requires access to particular platform units.

It is worth noting that in order to access the Java elements of the lsFusion system directly, you must first obtain a link to an object that will have interfaces for finding these Java elements. This is usually done in one of two ways:

1.  If the initial call comes from the lsFusion system via the [Java interaction](Access_to_an_internal_system_INTERNAL_FORMULA.md#javato) mechanism, the action object "through which" the call is done may be used as the "search object" (the class of this action must be inherited from `lsfusion.server.physics.dev.integration.internal.to.InternalAction`, which in turn has all the required interfaces).
2.  If the object from whose method the lsFusion system must be accessed is a Spring bean, a link to the business logic object can be obtained using dependency injection (the bean is accordingly called `businessLogics`).

### SQL interaction

Systems that have access to the SQL server of the lsFusion system (one such system, for example, is the SQL server itself) can directly access [tables](Tables.md) and [fields](Materializations.md) created by the lsFusion system using SQL server means. It should be kept in mind that while reading data is relatively safe (except for possible deletion/modification of tables and their fields), when writing data no [events](Events.md) will be triggered (including all elements that use them - [constraints](Constraints.md), [aggregations](Aggregations.md), etc.), and also no [materializations](Materializations.md) will be recalculated. For this reason writing data directly to lsFusion system tables is highly discouraged. If doing so is necessary, all of the above factors should be taken into account.

### Examples
```java
package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.remote.UserInfo;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.AuthenticationLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;

public class GenerateLoginPasswordActionProperty extends ScriptingActionProperty {

    private LCP email;
    private LCP loginCustomUser;
    private LCP sha256PasswordCustomUser;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordActionProperty(AuthenticationLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        this.email = findProperty("email[Contact]");
        this.loginCustomUser = findProperty("login[CustomUser]");
        this.sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject userObject = context.getDataKeyValue(customUserInterface);

        String currentEmail = (String) email.read(context, userObject);

        String login;
        int indexMail;
        if(currentEmail != null && (indexMail = currentEmail.indexOf("@"))>=0)
            login = currentEmail.substring(0, indexMail);
        else
            login = "login" + userObject.object;

        Random rand = new Random();
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        String password = "";
        for(int i=0;i<8;i++)
            password += chars.charAt(rand.nextInt(chars.length()));

        if (loginCustomUser.read(context, userObject) == null)
            loginCustomUser.change(login, context, userObject);
        String sha256Password = BaseUtils.calculateBase64Hash("SHA-256", password, UserInfo.salt);
        sha256PasswordCustomUser.change(sha256Password, context, userObject);
    }
}
```