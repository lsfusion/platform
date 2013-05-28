@echo off

setlocal
set POSTGRE_DIR=d:\Program Files\PostgreSQL\9.2
set tempfile="%TEMP%\_copy_db_temp.backup"
set fromdb=%1
set todb=%2

chcp 1251

if "%fromdb%" == "" (
    echo "usage db_copy <from_dbname> <to_dbname>"
) else (
    if "%todb%" == "" (
        echo "usage db_copy <from_dbname> <to_dbname>"
    ) else (
        "%POSTGRE_DIR%\bin\pg_dump.exe" -v -F c -U postgres -f %tempfile% %fromdb%

        "%POSTGRE_DIR%\bin\dropdb.exe" -U postgres %todb%
        "%POSTGRE_DIR%\bin\createdb.exe" -U postgres %todb%
        "%POSTGRE_DIR%\bin\pg_restore.exe" -v -U postgres -d %todb% %tempfile%
        
        del /Q %tempfile%
    )
)
