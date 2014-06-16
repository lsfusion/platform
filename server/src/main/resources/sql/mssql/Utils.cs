
CREATE FUNCTION Hash(@a int) RETURNS int AS EXTERNAL NAME [${assembly.name}].Utils.Hash;
CREATE FUNCTION RandFromInt(@a int) RETURNS double precision AS EXTERNAL NAME [${assembly.name}].Utils.RandFromInt;

SQL DROP

IF OBJECT_ID(N'Hash', N'FS') IS NOT NULL DROP FUNCTION Hash;
IF OBJECT_ID(N'RandFromInt', N'FS') IS NOT NULL DROP FUNCTION RandFromInt;

REFS

System.dll
System.Data.dll
System.Xml.dll

CODE

//CREATE FUNCTION ${type.name}(${declare.sqlprms}) RETURNS ${type.name} AS EXTERNAL NAME [${assembly.name}].${type.name}.new${type.name}; 

//IF OBJECT_ID(N'${type.name}', N'FS') IS NOT NULL DROP FUNCTION ${type.name};

//IF TYPE_ID(N'${type.name}') IS NOT NULL DROP TYPE ${type.name};

using System;
using System.Data;
using System.IO;
using System.Data.SqlTypes;
using Microsoft.SqlServer.Server;
using System.Text;

public struct Utils
{
    public static int Hash(int a) {
        a = (a ^ 61) ^ (a >> 16);
        a = a + (a << 3);
        a = a ^ (a >> 4);
        a = a * 0x27d4eb2d;
        a = a ^ (a >> 15);
        return a;
    }

    public static double RandFromInt(int a) {
        return (((double)Hash(a)) % 1000000) / 1000000; 
    }
}