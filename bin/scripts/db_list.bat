@echo off

setlocal
set POSTGRE_DIR=d:\Program Files\PostgreSQL\9.2
set db=%1

chcp 1251

"%POSTGRE_DIR%\bin\psql.exe" -U postgres --list
