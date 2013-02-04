@echo off

setlocal
set POSTGRE_DIR=d:\Program Files\PostgreSQL\9.2
set db=%1

if "%db%" == "" (
    echo "usage db_clean <dbname>"
) else (
    "%POSTGRE_DIR%\bin\dropdb.exe" -U postgres %db%
    "%POSTGRE_DIR%\bin\createdb.exe" -U postgres %db%
)
