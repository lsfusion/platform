set JAVA_HOME=JAVA
call "..\install-bin\apache-ant-1.9.2\bin\ant" -verbose -f configure.xml %* >>configure.log 2>&1
