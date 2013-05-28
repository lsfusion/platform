using System;
using System.IO;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using System.Data;
using System.Data.SqlTypes;
using System.Data.Sql;
using System.Text.RegularExpressions;
using Microsoft.SqlServer.Server;

    [Serializable]
    [Microsoft.SqlServer.Server.SqlUserDefinedAggregate(
        Microsoft.SqlServer.Server.Format.UserDefined, //use clr serialization to serialize the intermediate result
        IsInvariantToNulls = true,//optimizer property
        IsInvariantToDuplicates = false,//optimizer property
        IsInvariantToOrder = false,//optimizer property
        MaxByteSize = 8000)//maximum size in bytes of persisted value
        ]
    public class Concatenate : Microsoft.SqlServer.Server.IBinarySerialize
    {
        /// <summary>
        /// The variable that holds the intermediate result of the concatenation
        /// </summary>
        private StringBuilder intermediateResult;

        /// <summary>
        /// Initialize the internal data structures
        /// </summary>
        public void Init()
        {
            intermediateResult = new StringBuilder();
        }

        /// <summary>
        /// Accumulate the next value, nop if the value is null
        /// </summary>
        /// <param name="value"></param>
        public void Accumulate(SqlString value)
        {

            if (value.IsNull)
            {
                return;
            }
            intermediateResult.Append(value.Value).Append(',');

        }

        /// <summary>
        /// Merge the partially computed aggregate with this aggregate.
        /// </summary>
        /// <param name="other"></param>
        public void Merge(Concatenate other)
        {
            intermediateResult.Append(other.intermediateResult);
        }

        /// <summary>
        /// Called at the end of aggregation, to return the results of the aggregation
        /// </summary>
        /// <returns></returns>
        public SqlString Terminate()
        {
            string output = string.Empty;
            //delete the trailing comma, if any
            if (intermediateResult != null && intermediateResult.Length > 0)
                output = intermediateResult.ToString(0, intermediateResult.Length - 1);
            return new SqlString(output);
        }

        public void Read(BinaryReader r)
        {
            if (r == null) throw new ArgumentNullException("r");
            intermediateResult = new StringBuilder(r.ReadString());
        }

        public void Write(BinaryWriter w)
        {
            if (w == null) throw new ArgumentNullException("w");
            w.Write(intermediateResult.ToString());
        }
    }