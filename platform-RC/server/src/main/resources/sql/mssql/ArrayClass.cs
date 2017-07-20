
CREATE TYPE dbo.${type.name} EXTERNAL NAME ${assembly.name}.[${type.name}];

CREATE FUNCTION ${type.name}(@input ${value.sqltype}) RETURNS ${type.name} AS EXTERNAL NAME [${assembly.name}].${type.name}.new${type.name};

CREATE AGGREGATE ${aggr.name}(@input ${type.name}) RETURNS ${type.name} EXTERNAL NAME [${assembly.name}].[${aggr.name}];

SQL DROP

IF OBJECT_ID(N'${aggr.name}', N'AF') IS NOT NULL DROP AGGREGATE ${aggr.name};

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
public struct ${type.name} : INullable, IBinarySerialize
{
    private bool is_Null;

    public ${type.name}(${value.type} val) {
        is_Null = false; 
        vals = new ${value.type} [] {val}; 
    }

    public ${type.name}(${value.type}[] vals) {
        is_Null = false; 
        this.vals = vals; 
    }

    public static ${type.name} new${type.name}(${value.type} val) {
        return new ${type.name}(val);
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
	    pt.vals = new ${value.type}[]{};
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
	    foreach (${value.type} val in vals)
	    {
	    	builder.Append(val).Append(",");
	    }
//            ${append.fields}
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
  //      ${parse.fields}

        return pt;
    }

    private ${value.type}[] vals;

    public ${type.name} Copy() {
        ${value.type}[] result = new ${value.type}[vals.Length];
        Array.Copy(vals, result, vals.Length);
        return new ${type.name}(result);
    }

    [SqlMethod(OnNullCall = false)]
    public bool Contains(${value.type} element) {
        for(int i=0;i<vals.Length;i++)
            if(element.Equals(vals[i])) 
                return true;
        return false;
    }

    public void mAdd(${type.name} add) {
        if (IsNull) {
            is_Null = false;
            vals = new ${value.type}[add.vals.Length];
            Array.Copy(add.vals, vals, add.vals.Length);
            return;
        }
        
        if (add.IsNull)
            return;            
    
        ${value.type}[] result = new ${value.type}[vals.Length + add.vals.Length];
        Array.Copy(vals, result, vals.Length);
        int num = vals.Length;
        foreach(${value.type} addVal in add.vals) {
            if(!Contains(addVal))
                result[num++] = addVal;
        }
        Array.Resize(ref result, num);
    	vals = result;
    }

    [SqlMethod(OnNullCall = false)]
    public ${type.name} Add(${type.name} add) {
        if (IsNull)
            return add;
        
        if (add.IsNull)
            return this;
                        
        ${type.name} result = Copy();
        result.mAdd(add);
        return result;
    }
       
    // just in case for aggregates
    public ${type.name} Immutable() {
        return Copy();
    }
    
    public void Read(BinaryReader r)
    {
        is_Null = !r.ReadBoolean();
        
        int size = r.ReadInt32();
        vals = new ${value.type}[size];
        for(int i=0;i<size;i++) {
    		${ser.read}
        }
    }

    public void Write(BinaryWriter w)
    {
        w.Write(!IsNull);
            
        w.Write(vals.Length);
        for(int i=0;i<vals.Length;i++) {
    		${ser.write}
        }
    }
}

[Serializable]
[Microsoft.SqlServer.Server.SqlUserDefinedAggregate(
    Microsoft.SqlServer.Server.Format.UserDefined, //use clr serialization to serialize the intermediate result
    IsInvariantToNulls = true,//optimizer property
    IsInvariantToDuplicates = true,//optimizer property
    IsInvariantToOrder = true,//optimizer property
    MaxByteSize = 8000)//maximum size in bytes of persisted value
    ]
public class ${aggr.name} : Microsoft.SqlServer.Server.IBinarySerialize
{

    private ${type.name} intermediateResult;
     
//     static void Main(string[] references)
//     {
//          ${aggr.name} test = new ${aggr.name}();
// //         Console.WriteLine(test.Terminate());
//          test.Init();
//          
//          Console.WriteLine(new ${type.name}(new ${value.type}("aaaa")));
//          test.Accumulate(new ${type.name}(new ${value.type}("aaaa")));
//          Console.WriteLine(test.Terminate());
//          test.Accumulate(new ${type.name}(new ${value.type}("bbbb")));
//          test.Accumulate(new ${type.name}(new ${value.type}("dddd")));
//          test.Accumulate(new ${type.name}(new ${value.type}("eeee")));
//          test.Accumulate(new ${type.name}(new ${value.type}("aaaa")));
//          test.Accumulate(new ${type.name}(new ${value.type}("aaaa")));
//          
//          Console.WriteLine(test.Terminate());
//          
//          ${aggr.name} t2 = new ${aggr.name}();              
//          t2.Init();
//          
//          t2.Accumulate(new ${type.name}(new ${value.type}("pppp")));
//          t2.Accumulate(new ${type.name}(new ${value.type}("dddd")));
//          t2.Accumulate(new ${type.name}(new ${value.type}("llll")));
//
//          Console.WriteLine(t2.Terminate());
//
//          byte[] byteArray = new byte[100000];
//          Stream stream = new MemoryStream(byteArray);
//          t2.Write(new BinaryWriter(stream));
//          t2.Read(new BinaryReader(new MemoryStream(byteArray)));
//          
//          Console.WriteLine(t2.Terminate());
//          
//          test.Merge(t2);
//          
//          Console.WriteLine(test.Terminate());
//     }

    /// <summary>
    /// Initialize the internal data structures
    /// </summary>
    public void Init()
    {
        intermediateResult = ${type.name}.Null;
    }

    /// <summary>
    /// Accumulate the next value, nop if the value is null
    /// </summary>
    public void Accumulate(${type.name} value)
    {
        intermediateResult.mAdd(value);
    }

    /// <summary>
    /// Merge the partially computed aggregate with this aggregate.
    /// </summary>
    public void Merge(${aggr.name} other)
    {
        intermediateResult.mAdd(other.intermediateResult);
    }

    /// <summary>
    /// Called at the end of aggregation, to return the results of the aggregation
    /// </summary>
    /// <returns></returns>
    public ${type.name} Terminate()
    {
	    return intermediateResult.Copy();
    }
    
    public void Read(BinaryReader r)
    {
        intermediateResult = new ${type.name}();
        intermediateResult.Read(r);
    }

    public void Write(BinaryWriter w)
    {
        intermediateResult.Write(w);
    }
}
