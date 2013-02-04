@echo off

setlocal
set POSTGRE_DIR=d:\Program Files\PostgreSQL\9.2
set db=%1

if "%db%" == "" (
    echo "usage db_drop <dbname>"
) else (
    "%POSTGRE_DIR%\bin\dropdb.exe" -U postgres %db%
)
