---
title: 'Обращение из внутренней системы'
---

### Java-взаимодействие

В рамках такого типа взаимодействия, внутренняя система может обращаться непосредственно к Java элементам lsFusion системы (как к обычным Java объектам). Таким образом, можно выполнять все те же операции как и с использованием сетевых протоколов, но при этом избежать существенного оверхеда такого взаимодействия (например, на сериализацию параметров / десериализацию результата и т.п). Кроме того, такой способ общения гораздо удобнее и эффективнее, если взаимодействие очень тесное (то есть в процессе выполнения одной операции требует постоянного обращения в обе стороны - от lsFusion системы к другой системе и обратно) и / или требует доступа к специфическим узлам платформы.

Стоит отметить, что для того чтобы обращаться к Java элементам lsFusion системы напрямую, нужно предварительно получить ссылку на некоторый объект, у которого будут интерфейсы по поиску этих Java элементов. Как правило это делается одним из двух способов:

1.  Если первоначально обращение идет из lsFusion системы через механизм [Java-взаимодействия](Access_to_an_internal_system_INTERNAL_FORMULA.md#javato), то в качестве "объекта поиска" можно использовать объект действия, "через которое" идет это обращение (класс этого действия, должен наследоваться от `lsfusion.server.physics.dev.integration.internal.to.InternalAction`, у которого, в свою очередь, есть все необходимые интерфейсы).
2.  Если объект, из метода которого необходимо обратиться к lsFusion системе, является Spring bean'ом, то ссылку на объект бизнес-логики можно получить используя dependency injection (соответственно bean называется `businessLogics`).

### SQL-взаимодействие

Системы имеющие доступ к SQL-серверу lsFusion-системы (одной из таких систем, к примеру, является сам SQL-сервер), могут обращаться непосредственно к [таблицам](Tables.md) и [полям](Materializations.md), созданным lsFusion-системой, средствами SQL-сервера. При этом необходимо учитывать что, если чтение данных относительно безопасно (за исключением возможного удаления / изменения таблиц и их полей), то при записи данных не будут вызваны никакие [события](Events.md) (и соответственно все элементы их использующие - [ограничения](Constraints.md), [агрегации](Aggregations.md) и т.п.), а также не будут пересчитаны никакие [материализации](Materializations.md). Поэтому записывать данные напрямую в таблицы lsFusion-системы крайне не рекомендуется, а если это все же необходимо, важно учесть все вышеупомянутые особенности.

### Примеры
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