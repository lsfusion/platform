!define ARCH 32

!define JAVA_INSTALLER "jdk-7u45-windows-i586.exe"
!define PG_INSTALLER "postgresql-9.2.5-1-windows.exe"
!define IDEA_INSTALLER "ideaIC-13.0.1.exe"
!define IDEA_PLUGIN "lsfusion-idea-plugin.zip"
!define IDEA_EXE "idea.exe"
!define TOMCAT_ARCHIVE "apache-tomcat-7.0.47-windows-x86.zip"
!define ANT_ARCHIVE "apache-ant-1.9.2.zip"

!ifndef OUT_FILE
;    !define OUT_FILE "lsfusion-setup-x32.exe"
    !define OUT_FILE "x32.exe"
!endif

!include Installer.nsh
