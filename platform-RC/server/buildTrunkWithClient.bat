set BUILD_DIR=%~dp0
call buildTrunk

cd %BUILD_DIR%\..\desktop-client
call assemble

cd %BUILD_DIR%\..\web-client
call assemble

cd %BUILD_DIR%