
CREATE AGGREGATE ${aggr.name}(@input ${value.sqltype}, @order ${order.sqltype}) RETURNS ${value.sqltype} EXTERNAL NAME [${assembly.name}].[${aggr.name}];

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
        IsInvariantToNulls = false,//optimizer property
        IsInvariantToDuplicates = true,//optimizer property
        IsInvariantToOrder = true,//optimizer property
        MaxByteSize = 8000)//maximum size in bytes of persisted value
        ]
    public class ${aggr.name} : Microsoft.SqlServer.Server.IBinarySerialize
    {
        /// <summary>
        /// The variable that holds the intermediate result of the concatenation
        /// </summary>
        private ${value.type} lastValue;
        private ${order.type} lastOrder;
        bool notEmpty;
         
//         static void Main(string[] references)
//         {
//              ${aggr.name} test = new ${aggr.name}();
//              Console.WriteLine(test.Terminate());
//              test.Init();
//              
//              test.Accumulate(new ${value.type}("aaaa"), new ${order.type}(5));
//              test.Accumulate(new ${value.type}("bbbb"), new ${order.type}(3));
//              test.Accumulate(new ${value.type}("dddd"), new ${order.type}(7));
//              test.Accumulate(new ${value.type}("eeee"), new ${order.type}(3));
//              
//              Console.WriteLine(test.Terminate());
//              
//              ${aggr.name} t2 = new ${aggr.name}();              
//              t2.Init();
//              
//              t2.Accumulate(new ${value.type}("pppp"), new ${order.type}(5));
//              t2.Accumulate(new ${value.type}("zzzz"), new ${order.type}(4));
//              t2.Accumulate(new ${value.type}("llll"), new ${order.type}(15));
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
            notEmpty = false;
        }

        /// <summary>
        /// Accumulate the next value, nop if the value is null
        /// </summary>
        public void Accumulate(${value.type} value, ${order.type} order)
        {

            if(!notEmpty || (order.CompareTo(lastOrder) >= 0)) {
                lastValue = value;
                lastOrder = order;
            }
            notEmpty = true;
        }

        /// <summary>
        /// Merge the partially computed aggregate with this aggregate.
        /// </summary>
        public void Merge(${aggr.name} other)
        {
            if(!other.notEmpty) {
                return;
            }
        
            if(!notEmpty || other.lastOrder.CompareTo(lastOrder) >= 0) {
                lastValue = other.lastValue;
                lastOrder = other.lastOrder;
            }
            notEmpty = true;
        }

        /// <summary>
        /// Called at the end of aggregation, to return the results of the aggregation
        /// </summary>
        /// <returns></returns>
        public ${value.type} Terminate()
        {
            if (notEmpty)
                return lastValue;
            return ${value.type}.Null;
        }
        
        private static void SerializeToBytes<T>(BinaryWriter writer, T item)
        {
//            var formatter = new BinaryFormatter();
//            formatter.Serialize(writer.BaseStream, item);
            BinaryFormatter bf = new BinaryFormatter();
            MemoryStream ms = new MemoryStream();
            bf.Serialize(ms, item);
            byte[] ba = ms.ToArray();
            
            writer.Write(ba.Length);
            writer.Write(ba);
        }
                
        private static object DeserializeFromBytes(BinaryReader reader)
        {            
            byte[] arrBytes = reader.ReadBytes(reader.ReadInt32());
            
            MemoryStream memStream = new MemoryStream();
            BinaryFormatter binForm = new BinaryFormatter();
            memStream.Write(arrBytes, 0, arrBytes.Length);
            memStream.Seek(0, SeekOrigin.Begin);
            Object obj = (Object) binForm.Deserialize(memStream);
            return obj;
//            var formatter = new BinaryFormatter();
//            return formatter.Deserialize(reader.BaseStream);
        }

        public void Read(BinaryReader r)
        {
            if (r == null) throw new ArgumentNullException("r");
//            lastValue = (${value.type})DeserializeFromBytes(r);
//            lastOrder = (${order.type})DeserializeFromBytes(r);
//            lastValue = new ${value.type}();
//            lastValue.Read(r);
//            lastOrder = new ${order.type}();
//            lastOrder.Read(r);
            ${ser.read}

            notEmpty = r.ReadBoolean();
        }

        public void Write(BinaryWriter w)
        {
            if (w == null) throw new ArgumentNullException("w");
//            SerializeToBytes(w, lastValue);
//            SerializeToBytes(w, lastOrder);
//            lastValue.Write(w);
//            lastOrder.Write(w);

            ${ser.write}
            w.Write(notEmpty);
        }
    }
