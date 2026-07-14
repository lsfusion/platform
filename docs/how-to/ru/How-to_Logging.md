---
slug: "/How-to_Logging"
title: 'How-to: Логирование'
---

Для логирования платформа использует библиотеку [log4j](https://logging.apache.org/log4j/1.x/). Все логи сервера приложений пишутся в папку `logs` его рабочей директории. Полный список стандартных логов для каждого компонента платформы (сервер приложений, веб-сервер, десктоп-клиент) приведен в статье [Журналы и логи](../paradigm/Journals_and_logs.md#logs).

Писать в логи можно как из lsFusion-кода, так и из Java-кода.

## Пример 1

### Условие

Нужно логировать ход обмена с внешней системой в отдельный файл.

### Решение

```lsf
REQUIRE Utils;

error = DATA LOCAL TEXT ();

exchange 'Обмен' () {
    printToLog('Обмен запущен', 'exchange');

    // ... логика обмена, при ошибке записывающая её текст в error() ...

    IF error() THEN
        printToLog('Ошибка обмена: ' + error(), 'exchange', 'error');
    ELSE
        printToLog('Обмен успешно завершен', 'exchange');
}
```

Для записи в лог используется действие `printToLog`, объявленное в системном модуле [`Utils`](../paradigm/System_Utils.md). Его перегрузки:

- `printToLog[TEXT, STRING, STRING]` — параметры: текст сообщения, имя логгера, уровень.
- `printToLog[TEXT, STRING]` — уровень `'info'`.
- `printToLog[TEXT]` — имя логгера `'system'`, уровень `'info'`.

Поддерживаемые уровни: `'info'`, `'warn'`, `'error'`.

Имя логгера преобразуется в имя log4j-логгера по схеме `<Имя>Logger` (первая буква капитализируется): в примере выше сообщения пойдут в логгер `ExchangeLogger`. Если логгер с таким именем не описан в конфигурации log4j, платформа автоматически создаст для него appender, пишущий в файл `logs/<имя>.log` (в примере — `logs/exchange.log`). Если же логгер описан в конфигурации (см. [Пример 4](#пример-4)), сообщения пойдут туда, куда он настроен.

Имени `'system'` соответствует логгер `SystemLogger`: его сообщения уровня `'info'` попадают в `logs/stdout.log`, а `'warn'` и `'error'` — в `logs/stderr.log`.

## Пример 2

### Условие

Нужно показать пользователю отладочное сообщение, не прерывая его работу модальным диалогом.

### Решение

```lsf
finishOrder (Order o) {
    // ... проведение заказа ...

    MESSAGE 'Заказ ' + number(o) + ' проведен' LOG;
}
```

[Оператор `MESSAGE`](../language/MESSAGE_operator.md) с типом `LOG` выводит сообщение в окно `System.log` клиентского приложения. В отличие от `printToLog`, такое сообщение показывается пользователю на клиенте и не записывается в файлы логов сервера.

## Пример 3

### Условие

В действии, создаваемом при помощи [оператора `INTERNAL`](../language/INTERNAL_operator.md), нужно писать сообщения в стандартные логи сервера.

### Решение

```lsf
runExchange 'Запустить обмен' INTERNAL 'RunExchange';
```

#### RunExchange.java
```java
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;

public class RunExchange extends InternalAction {

    public RunExchange(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ServerLoggers.systemLogger.info("Обмен запущен");
        try {
            // ... логика обмена ...
        } catch (Exception e) {
            ServerLoggers.systemLogger.error("Ошибка обмена", e);
        }
    }
}
```

Класс `lsfusion.server.physics.admin.log.ServerLoggers` содержит статические ссылки на все стандартные логгеры сервера приложений. Наиболее часто используемые из них:

|Логгер|Лог-файл|Назначение|
|---|---|---|
|`systemLogger`|`stdout.log` / `stderr.log`|общесистемные сообщения (`warn` и `error` попадают в `stderr.log`)|
|`serviceLogger`|`service.log`|сервисные операции|
|`sqlLogger`|`sql.log`|обращения к серверу БД|
|`remoteLogger`|`server-remote.log`|обращения к серверу приложений|
|`mailLogger`|`mail.log`|отправка и получение почты|
|`importLogger`|—|процессы импорта (файл настраивается в конфигурации проекта)|

## Пример 4

### Условие

Для процессов импорта нужен отдельный лог `logs/import.log` с ротацией по размеру.

### Решение

В Java-коде сообщения пишутся в логгер `ImportLogger` (можно использовать готовую ссылку `ServerLoggers.importLogger`):

```java
ServerLoggers.importLogger.info("Импорт завершен, обработано строк: " + count);
```

Конфигурация логгера задается в файле `log4j.xml`. Проект может положить в свои ресурсы (`src/main/resources`) собственную копию этого файла — как правило, она находится в classpath раньше платформенной и поэтому перекрывает её. В конфигурацию добавляются appender и категория:

```xml
<appender name="importlog" class="org.apache.log4j.rolling.RollingFileAppender">
    <param name="encoding" value="UTF-8" />
    <rollingPolicy class="org.apache.log4j.rolling.FixedWindowRollingPolicy">
        <param name="minIndex" value="1"/>
        <param name="maxIndex" value="9"/>
        <param name="activeFileName" value="logs/import.log"/>
        <param name="fileNamePattern" value="logs/import-%i.log.zip"/>
    </rollingPolicy>
    <triggeringPolicy class="org.apache.log4j.rolling.SizeBasedTriggeringPolicy">
        <param name="maxFileSize" value="10485760"/> <!-- 10MB -->
    </triggeringPolicy>
    <layout class="org.apache.log4j.EnhancedPatternLayout">
        <param name="ConversionPattern" value="%d{DATE} %5p %c{1} - %m%n%throwable{1000}"/>
    </layout>
</appender>

<category name="ImportLogger" additivity="false">
    <priority value="INFO"/>
    <appender-ref ref="importlog"/>
</category>
```

Этот же логгер автоматически становится доступен и из lsFusion-кода: вызов `printToLog[TEXT, STRING]` с именем логгера `'import'` запишет сообщение в настроенный выше `logs/import.log`.

## Дополнительно

- Список всех стандартных логов и папок, в которые они пишутся, — в статье [Журналы и логи](../paradigm/Journals_and_logs.md#logs).
- Отладочное логирование (debug-уровень сервера, логирование удаленных вызовов, планы запросов и т.п.) можно включать для отдельного пользователя — соответствующие флаги описаны в статье [Service](../paradigm/System_Service.md).
