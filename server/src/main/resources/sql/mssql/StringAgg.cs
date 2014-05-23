
CREATE AGGREGATE ${aggr.name}(@input nvarchar(max), @sep nvarchar(max), @order ${order.sqltype}) RETURNS nvarchar(max) EXTERNAL NAME [${assembly.name}].[${aggr.name}];

SQL DROP

IF OBJECT_ID(N'${aggr.name}', N'AF') IS NOT NULL DROP AGGREGATE ${aggr.name};

REFS

System.dll
System.Data.dll
System.Xml.dll

CODE

using System;
using System.IO;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using System.Data;
using System.Data.SqlTypes;
using System.Data.Sql;
using System.Text.RegularExpressions;
using System.Runtime.Serialization.Formatters.Binary;
using Microsoft.SqlServer.Server;

    [Serializable]
    [Microsoft.SqlServer.Server.SqlUserDefinedAggregate(
        Microsoft.SqlServer.Server.Format.UserDefined, //use clr serialization to serialize the intermediate result
        IsInvariantToNulls = true,//optimizer property
        IsInvariantToDuplicates = false,//optimizer property
        IsInvariantToOrder = false,//optimizer property
        MaxByteSize = 8000)//maximum size in bytes of persisted value
        ]
    public class ${aggr.name} : Microsoft.SqlServer.Server.IBinarySerialize
    {
        /// <summary>
        /// The variable that holds the intermediate result of the concatenation
        /// </summary>
        private SortedDictionary<${order.type}, StringBuilder> intermediateResult;
        private int lastSepLength;
         
//         static void Main(string[] references)
//         {
//              ${aggr.name} test = new ${aggr.name}();
//              Console.WriteLine(test.Terminate());
//              test.Init();
//              
//              test.Accumulate(new SqlString("aaaa"), new ${order.type}(5));
//              test.Accumulate(new SqlString("bbbb"), new ${order.type}(3));
//              test.Accumulate(new SqlString("dddd"), new ${order.type}(7));
//              test.Accumulate(new SqlString("eeee"), new ${order.type}(3));
//              
//              Console.WriteLine(test.Terminate());
//              
//              ${aggr.name} t2 = new ${aggr.name}();              
//              t2.Init();
//              
//              t2.Accumulate(new SqlString("pppp"), new ${order.type}(5));
//              t2.Accumulate(new SqlString("zzzz"), new ${order.type}(4));
//              t2.Accumulate(new SqlString("llll"), new ${order.type}(5));
//
//              Console.WriteLine(t2.Terminate());
//
//              byte[] byteArray = new byte[100000];
//              Stream stream = new MemoryStream(byteArray);
//              t2.Write(new BinaryWriter(stream));
//              t2.Read(new BinaryReader(new MemoryStream(byteArray)));
//              
//              Console.WriteLine(t2.Terminate());
//              
//              test.Merge(t2);
//              
//              Console.WriteLine(test.Terminate());
//         }

        /// <summary>
        /// Initialize the internal data structures
        /// </summary>
        public void Init()
        {
            intermediateResult = new SortedDictionary<${order.type}, StringBuilder>();
        }

        /// <summary>
        /// Accumulate the next value, nop if the value is null
        /// </summary>
        public void Accumulate(SqlString value, SqlString sep, ${order.type} order)
        {

            if (value.IsNull)
            {
                return;
            }
            
            StringBuilder list;
            if(!intermediateResult.TryGetValue(order, out list)) {
                list = new StringBuilder();
                intermediateResult.Add(order, list);
            }
            list.Append(value.Value).Append(sep.Value);
            lastSepLength = sep.Value.Length;
        }

        /// <summary>
        /// Merge the partially computed aggregate with this aggregate.
        /// </summary>
        public void Merge(${aggr.name} other)
        {
            foreach( KeyValuePair<${order.type}, StringBuilder> kvp in other.intermediateResult )
            {
                StringBuilder list;
                if(!intermediateResult.TryGetValue(kvp.Key, out list)) {
                    intermediateResult.Add(kvp.Key, new StringBuilder(kvp.Value.ToString()));
                } else
                    list.Append(kvp.Value.ToString());
            }
            if(lastSepLength == 0)
                lastSepLength = other.lastSepLength; 
        }

        /// <summary>
        /// Called at the end of aggregation, to return the results of the aggregation
        /// </summary>
        /// <returns></returns>
        public SqlString Terminate()
        {
            //delete the trailing comma, if any
            if (intermediateResult != null && intermediateResult.Count > 0) {
                StringBuilder result = new StringBuilder();
                foreach( StringBuilder sb in intermediateResult.Values )
                    result.Append(sb.ToString());
                return new SqlString(result.ToString(0, result.Length - lastSepLength));
            }
            return SqlString.Null;
        }
        
        private static void SerializeToBytes<T>(BinaryWriter writer, T item)
        {
            var formatter = new BinaryFormatter();
            formatter.Serialize(writer.BaseStream, item);
        }
                
        private static object DeserializeFromBytes(BinaryReader reader)
        {
            var formatter = new BinaryFormatter();
            return formatter.Deserialize(reader.BaseStream);
        }

        public void Read(BinaryReader r)
        {
            if (r == null) throw new ArgumentNullException("r");
            intermediateResult = new SortedDictionary<${order.type}, StringBuilder>();
            for(int i=0, size = r.ReadInt32(); i < size; i++) {
                ${order.type} ov;
                ${ser.read}
                intermediateResult.Add(ov, new StringBuilder(r.ReadString()));                
            }
            lastSepLength = r.ReadInt32();
        }

        public void Write(BinaryWriter w)
        {
            if (w == null) throw new ArgumentNullException("w");
            w.Write(intermediateResult.Count);
            foreach( KeyValuePair<${order.type}, StringBuilder> kvp in intermediateResult ) {
                ${order.type} ov = kvp.Key;
                ${ser.write}
                w.Write(kvp.Value.ToString());
            }
            w.Write(lastSepLength);
        }
    }
