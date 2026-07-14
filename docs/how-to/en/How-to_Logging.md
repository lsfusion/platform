---
slug: "/How-to_Logging"
title: 'How-to: Logging'
---

The platform uses the [log4j](https://logging.apache.org/log4j/1.x/) library for logging. All application server logs are written to the `logs` folder of its working directory. The full list of standard logs for each platform component (application server, web server, desktop client) is given in the [Journals and logs](../paradigm/Journals_and_logs.md#logs) article.

You can write to the logs both from lsFusion code and from Java code.

## Example 1

### Task

We need to log the progress of an exchange with an external system to a separate file.

### Solution

```lsf
REQUIRE Utils;

error = DATA LOCAL TEXT ();

exchange 'Exchange' () {
    printToLog('Exchange started', 'exchange');

    // ... exchange logic that writes the error text to error() on failure ...

    IF error() THEN
        printToLog('Exchange error: ' + error(), 'exchange', 'error');
    ELSE
        printToLog('Exchange finished successfully', 'exchange');
}
```

The `printToLog` action, declared in the [`Utils`](../paradigm/System_Utils.md) system module, is used for writing to the log. Its overloads:

- `printToLog[TEXT, STRING, STRING]` — parameters: the message text, the logger name, the level.
- `printToLog[TEXT, STRING]` — the `'info'` level.
- `printToLog[TEXT]` — the `'system'` logger name, the `'info'` level.

Supported levels: `'info'`, `'warn'`, `'error'`.

The logger name is converted to a log4j logger name using the `<Name>Logger` scheme (the first letter is capitalized): in the example above the messages go to the `ExchangeLogger` logger. If a logger with this name is not described in the log4j configuration, the platform automatically creates an appender for it that writes to the `logs/<name>.log` file (in this example — `logs/exchange.log`). If the logger is described in the configuration (see [Example 4](#example-4)), the messages go wherever it is configured to write.

The `'system'` name corresponds to the `SystemLogger` logger: its messages of the `'info'` level end up in `logs/stdout.log`, while `'warn'` and `'error'` ones go to `logs/stderr.log`.

## Example 2

### Task

We need to show a debug message to the user without interrupting their work with a modal dialog.

### Solution

```lsf
finishOrder (Order o) {
    // ... posting the order ...

    MESSAGE 'Order ' + number(o) + ' has been posted' LOG;
}
```

The [`MESSAGE` operator](../language/MESSAGE_operator.md) with the `LOG` type shows the message in the `System.log` window of the client application. Unlike `printToLog`, such a message is shown to the user on the client and is not written to the server log files.

## Example 3

### Task

In an action created with the [`INTERNAL` operator](../language/INTERNAL_operator.md), we need to write messages to the standard server logs.

### Solution

```lsf
runExchange 'Run exchange' INTERNAL 'RunExchange';
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
        ServerLoggers.systemLogger.info("Exchange started");
        try {
            // ... exchange logic ...
        } catch (Exception e) {
            ServerLoggers.systemLogger.error("Exchange error", e);
        }
    }
}
```

The `lsfusion.server.physics.admin.log.ServerLoggers` class contains static references to all standard loggers of the application server. The most commonly used ones:

|Logger|Log file|Purpose|
|---|---|---|
|`systemLogger`|`stdout.log` / `stderr.log`|system-wide messages (`warn` and `error` go to `stderr.log`)|
|`serviceLogger`|`service.log`|service operations|
|`sqlLogger`|`sql.log`|database server calls|
|`remoteLogger`|`server-remote.log`|application server calls|
|`mailLogger`|`mail.log`|sending and receiving mail|
|`importLogger`|—|import processes (the file is configured in the project configuration)|

## Example 4

### Task

Import processes need a separate `logs/import.log` log with size-based rotation.

### Solution

In Java code, messages are written to the `ImportLogger` logger (the ready-made `ServerLoggers.importLogger` reference can be used):

```java
ServerLoggers.importLogger.info("Import finished, rows processed: " + count);
```

The logger configuration is defined in the `log4j.xml` file. A project can put its own copy of this file into its resources (`src/main/resources`) — as a rule, it comes earlier in the classpath than the platform one and therefore overrides it. An appender and a category are added to the configuration:

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

The same logger automatically becomes available from lsFusion code as well: calling `printToLog[TEXT, STRING]` with the `'import'` logger name writes the message to the `logs/import.log` configured above.

## See also

- The list of all standard logs and the folders they are written to — in the [Journals and logs](../paradigm/Journals_and_logs.md#logs) article.
- Debug logging (server debug level, remote call logging, query plans, etc.) can be enabled for an individual user — the corresponding flags are described in the [Service](../paradigm/System_Service.md) article.
