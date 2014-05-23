
CREATE TYPE dbo.${type.name} EXTERNAL NAME ${assembly.name}.[${type.name}];

CREATE FUNCTION ${type.name}(${declare.sqlprms}) RETURNS ${type.name} AS EXTERNAL NAME [${assembly.name}].${type.name}.new${type.name};
SQL DROP

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

[Serializable]
[Microsoft.SqlServer.Server.SqlUserDefinedType(Format.UserDefined,
     IsByteOrdered=true, MaxByteSize=8000)]
public struct ${type.name} : INullable, IComparable, IBinarySerialize
{
    private bool is_Null;
    ${declare.fields}
    
    public ${type.name}(${declare.prms}) {
        is_Null = false; 
        ${assign.fields}
    }
    
    public static ${type.name} new${type.name}(${declare.prms}) {
        return new ${type.name}(${pass.prms});
    }
    
    public int CompareTo(object obj) {
        if(!(obj is ${type.name}))
            return 1;
        
        ${type.name} typeObj = (${type.name})obj;
        if(IsNull)
            return -1;
        if(typeObj.IsNull)
            return 1;

        int cmp;
        ${compare.fields}
        
        return 0;
    }
    
    public override bool Equals(object obj)
    {
        return this.CompareTo(obj) == 0;
    }
    
    public override int GetHashCode() {
    	if(is_Null)
    		return 0;
    	return ${hashcode.fields};
    }

    public bool IsNull
    {
        get
        {
            return (is_Null);
        }
    }

    public static ${type.name} Null
    {
        get
        {
            ${type.name} pt = new ${type.name}();
            pt.is_Null = true;
            return pt;
        }
    }

    // Use StringBuilder to provide string representation of UDT.
    public override string ToString()
    {
        // Since InvokeIfReceiverIsNull defaults to 'true'
        // this test is unneccesary if ${type.name} is only being called
        // from SQL.
        if (this.IsNull)
            return "NULL";
        else
        {
            StringBuilder builder = new StringBuilder();
            ${append.fields}
            return builder.ToString();
        }
    }

    [SqlMethod(OnNullCall = false)]
    public static ${type.name} Parse(SqlString s)
    {
        // With OnNullCall=false, this check is unnecessary if 
        // ${type.name} only called from SQL.
        if (s.IsNull)
            return Null;

        // Parse input string to separate out ${type.name}s.
        ${type.name} pt = new ${type.name}();
        string[] xy = s.Value.Split(",".ToCharArray());
        ${parse.fields}

        return pt;
    }


    ${declare.props}
    
    public void Read(BinaryReader r)
    {
        is_Null = !r.ReadBoolean();
        ${ser.read}
    }

    public void Write(BinaryWriter w)
    {
        w.Write(!IsNull);    
        ${ser.write}
    }
}