---
title: 'Внутренний вызов (INTERNAL)'
---

Платформа предоставляет возможность lsFusion-системе обращаться к коду, выполняющемуся внутри её собственных компонентов развёртывания — JVM сервера приложений, веб-клиенту пользователя либо БД, используемой платформой, — с использованием различных типов внутренних вызовов, как внутренний аналог [обращения к внешней системе](Access_to_an_external_system_EXTERNAL.md). Интерфейсом такого обращения является выполнение кода в среде целевого компонента с заданными параметрами и, при необходимости, возврат некоторых значений в качестве *результатов*, записываемых в заданные свойства (без параметров). Предполагается, что все объекты параметров и результатов являются объектами [встроенных классов](Built-in_classes.md).

## Типы внутренних вызовов

На данный момент в платформе поддерживаются следующие типы внутренних вызовов:

### Java - выполнение Java-кода в JVM сервера приложений {#java}

Для этого типа взаимодействия задаётся Java-класс — либо полным именем класса, скомпилированный класс которого должен быть доступен в classpath сервера приложений, либо встраиваемым Java-кодом, размещённым прямо в исходнике lsFusion (в этом случае платформа автоматически генерирует обрамляющий класс). Java-класс должен быть унаследован от `lsfusion.server.physics.dev.integration.internal.to.InternalAction`, и его метод `executeInternal(ExecutionContext context)` вызывается при каждом исполнении.

Классы параметров действия берутся из окружающего объявления либо из явного списка, указанного в месте объявления. По умолчанию действие не принимает `NULL`-значения параметров и молча пропускается при их наличии; приём `NULL` необходимо разрешать явно — либо ключевым словом `NULL` в [операторе `INTERNAL`](INTERNAL_operator.md), либо переопределением метода `allowNulls()` в самом классе.

Внутри Java-кода runtime платформы доступен через параметр `context` (текущая [сессия изменений](Change_sessions.md), значения параметров и окружение исполнения) и через разрешающие методы `InternalAction`. Возврат результатов в этом типе вызова не нуждается в отдельном механизме: Java-код пишет нужные значения непосредственно в свойства lsFusion из самого тела действия, в той же сессии изменений.

Внутри `executeInternal(ExecutionContext<ClassPropertyInterface> context)` — через сам класс `InternalAction` и через `context` — доступно:

- **Поиск элементов системы** через резолвящие методы `InternalAction`: `findProperty(name)` и `findAction(name)` (возвращают Java-обёртки `LP<?>` и `LA<?>` над [свойством](Properties.md) или [действием](Actions.md), найденным по [идентификатору](IDs.md#propertyid)), а также `findClass`, `findGroup`, `findForm`. Та же функциональность доступна через `context.getBL()` — корневой объект бизнес-логики (`BusinessLogics`), от которого можно дойти до конкретных [модулей логики](Modules.md) и далее до объявленных в них элементов.
- **Параметры действия** — соответствие интерфейсов и позиций параметров берётся из `interfaces` / `getOrderInterfaces()`. Для действия по умолчанию (без `NULL`) значение каждого параметра — гарантированно не-`NULL` `DataObject`, доступный через `context.getDataKeyValue(interface)` и сразу пригодный для передачи в `LP.read` / `LP.change`. Если действие принимает `NULL` (`INTERNAL ... NULL` либо переопределённый `allowNulls()`), нужно использовать `context.getKeyValue(interface)` (`ObjectValue`, может быть `NullValue`) или `context.getKeyObject(interface)` (исходное значение, может быть `null`) с явной обработкой `NULL`. Позиционные хелперы `InternalAction` — `getParamValue(i, context)` (`ObjectValue`) и `getParam(i, context)` (`Object`) — заворачивают эти же варианты.
- **Чтение значений свойств** — `lp.read(context, params...)` возвращает текущее значение свойства на переданных аргументах.
- **Изменение значений свойств** — `lp.change(value, context, params...)`. Изменения применяются к текущей сессии изменений атомарно с остальной транзакцией lsFusion и со всеми зависимыми [событиями](Events.md), [ограничениями](Constraints.md), [агрегациями](Aggregations.md) и [материализациями](Materializations.md).
- **Выполнение действий** — `la.execute(context, params...)`. Действие выполняется в той же сессии изменений и стеке вызовов, что и сам `InternalAction`.
- **Управление сессией и транзакцией** — `context.getSession()` и связанные с ней методы (`apply`, `cancel`, `setNoCancelInTransaction` и т.п.).
- **Сообщения и обратная связь пользователю** — `context.messageSuccess`, `context.messageError`, `context.delayUserInteraction(...)` и аналогичные методы.

Java-обращение к lsFusion-системе из объектов, которые сами не являются `InternalAction` (например, из Spring bean'ов через инъекцию `businessLogics`), описано в статье [обращение из внутренней системы](Access_from_an_internal_system.md).

### CLIENT - обращение к коду или файлу в веб-клиенте пользователя {#client}

Для этого типа взаимодействия задаётся имя клиентской цели — JavaScript-функции, уже загруженной в веб-клиенте пользователя, либо клиентского файла (входящего в поставку приложения или заданного во внешнем окружении в момент вызова — как правило, по URL, но также и другими file/path-ссылками, разрешаемыми на сервере приложений). К какой из двух целей относится конкретный вызов, определяется в момент вызова по виду имени ресурса. Скрипты, стили и шрифты обрабатываются клиентом специальным образом, соответствующим виду каждого из них; все остальные типы файлов, включая изображения, предоставляются клиенту как обобщённые файлы.

Клиентские вызовы по умолчанию асинхронные: сервер не ждёт завершения работы клиента. Синхронность нужно указывать явно; захват результата во встраиваемой форме дополнительно делает вызов синхронным.

Вспомогательные операции над уже загруженными файловыми ресурсами — например, выгрузка ранее загруженного скрипта или стиля — выражаются через зарезервированный префикс в имени ресурса; точный синтаксис описан в статье [оператора `INTERNAL`](INTERNAL_operator.md).

### DB - SQL против собственной БД платформы {#db}

Для этого типа взаимодействия задаётся SQL-команда как выражение свойства, вычисляемое в момент вызова. Подстановка параметров, табличные параметры, загрузка команды из ресурса classpath и запись результатов в свойства подчиняются тем же правилам, что и [`EXTERNAL SQL`](Access_to_an_external_system_EXTERNAL.md#sql) — отличия в том, что строка подключения не задаётся, а вызов выполняется внутри текущей [сессии изменений](Change_sessions.md), так что временные таблицы, транзакционное состояние и запись результатов откатываются или фиксируются вместе с этой сессией. Это также единственный поддерживаемый способ выполнять произвольный SQL против собственной БД платформы.

## Язык

Для объявления действия или записи оператора-действия, выполняющего внутренний вызов, используется [оператор `INTERNAL`](INTERNAL_operator.md).

## Примеры

```lsf
// Java-цель — скомпилированный InternalAction-класс
cmd '{utils.cmd}' (TEXT command, TEXT directory, BOOLEAN isClient, BOOLEAN wait)
    INTERNAL 'lsfusion.server.physics.admin.interpreter.action.RunCommandAction';

// Java-цель — встраиваемый Java-фрагмент, доступ к runtime платформы через `context`
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>;

// Клиентская цель — файловые ресурсы, загружаемые в веб-клиент при старте, с ожиданием каждого
onWebClientStarted() + {
    INTERNAL CLIENT WAIT 'plotly-3.0.1.min.js';
    INTERNAL CLIENT WAIT 'dashboard.js';
}

// Клиентская цель — JavaScript-функция, вызываемая синхронно, возвращаемое значение захватывается
getCookie(STRING name) {
    LOCAL cookie = STRING();
    INTERNAL CLIENT 'getCookie' PARAMS name TO cookie;
}

// Внутренняя SQL-цель — SQL, выполняющийся в текущей сессии изменений
loadPrices() {
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Мясо%';
    INTERNAL DB 'select price, barcode from $1' PARAMS exportFile() TO exportFile;
}
```

Пример Java-класса, унаследованного от `InternalAction`, — резолвит свойства в конструкторе через `findProperty`, читает и пишет их в `executeInternal` через `LP.read` / `LP.change`, передавая `context`:

```java
package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.UserInfo;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;

public class GenerateLoginPasswordAction extends InternalAction {

    private final LP<?> email;
    private final LP<?> loginCustomUser;
    private final LP<?> sha256PasswordCustomUser;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordAction(ScriptingLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);

        this.email = findProperty("email[Contact]");
        this.loginCustomUser = findProperty("login[CustomUser]");
        this.sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject userObject = context.getDataKeyValue(customUserInterface);

        String currentEmail = (String) email.read(context, userObject);

        String login;
        int indexMail;
        if (currentEmail != null && (indexMail = currentEmail.indexOf("@")) >= 0)
            login = currentEmail.substring(0, indexMail);
        else
            login = "login" + userObject.object;

        Random rand = new Random();
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++)
            password.append(chars.charAt(rand.nextInt(chars.length())));

        if (loginCustomUser.read(context, userObject) == null)
            loginCustomUser.change(login, context, userObject);
        String sha256Password = BaseUtils.calculateBase64Hash("SHA-256", password.toString(), UserInfo.salt);
        sha256PasswordCustomUser.change(sha256Password, context, userObject);
    }
}
```

Подключение этого класса со стороны lsFusion-кода:

```lsf
generateLoginPassword 'Сгенерировать логин и пароль' (CustomUser u)
    INTERNAL 'lsfusion.server.logics.property.actions.GenerateLoginPasswordAction';
```

Тип первого параметра конструктора должен совпадать с runtime-классом `LogicsModule`, из которого это действие подключается через `INTERNAL` (поиск конструктора идёт по точному классу): для большинства scripted-модулей это `ScriptingLogicsModule`, для модулей с собственным Java-подклассом — этот подкласс.
