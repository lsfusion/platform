@echo off

setlocal
set POSTGRE_DIR=d:\Program Files\PostgreSQL\9.2
set db=%1
set file=%2

if "%db%" == "" (
    echo "usage db_restore <dbname> [<filename>]"
) else (
    if "%file%" == "" (
        "%POSTGRE_DIR%\bin\dropdb.exe" -U postgres %db%
        "%POSTGRE_DIR%\bin\createdb.exe" -U postgres %db%
        "%POSTGRE_DIR%\bin\pg_restore.exe" -v -U postgres -d %db% "./backup/%db%.backup"
    ) else (
        "%POSTGRE_DIR%\bin\dropdb.exe" -U postgres %db%
        "%POSTGRE_DIR%\bin\createdb.exe" -U postgres %db%
        "%POSTGRE_DIR%\bin\pg_restore.exe" -v -U postgres -d %db% "./backup/%file%.backup"
    )
)
