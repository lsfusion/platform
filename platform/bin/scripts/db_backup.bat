@echo off

setlocal
set POSTGRE_DIR=d:\Program Files\PostgreSQL\9.2
set db=%1
set file=%2

if "%db%" == "" (
    echo "usage db_backup <dbname> [<filename>]"
) else (
    if "%file%" == "" (
        "%POSTGRE_DIR%\bin\pg_dump.exe" -v -F c -U postgres -f "./backup/%db%.backup" %db%
    ) else (
        "%POSTGRE_DIR%\bin\pg_dump.exe" -v -F c -U postgres -f "./backup/%file%.backup" %db%
    )
)