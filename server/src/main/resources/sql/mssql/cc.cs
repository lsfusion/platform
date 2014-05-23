using System;
using System.Collections.Generic;
using System.Text;

using System.CodeDom.Compiler;
using System.IO;
using Microsoft.CSharp;
using System.Reflection;
using System.Windows.Forms;

namespace DynaCode
{

class Program
{

    private static string ReadString(BinaryReader reader)
    {
	return System.Text.Encoding.Default.GetString(reader.ReadBytes(reader.ReadInt32()));
    }

    static void Main(string[] references)
    {

        CompilerParameters CompilerParams = new CompilerParameters();

        CompilerParams.TreatWarningsAsErrors = false;
        CompilerParams.GenerateExecutable = false;
        CompilerParams.CompilerOptions = "/optimize";

        CompilerParams.ReferencedAssemblies.AddRange(references);

	CSharpCodeProvider provider = new CSharpCodeProvider();

	BinaryReader reader = new BinaryReader(Console.OpenStandardInput());
	int size = reader.ReadInt32();

	FileStream fstream = null;

//	MessageBox.Show("" + size);

	for(int i=0;i<size;i++) {
		CompilerParams.OutputAssembly = ReadString(reader) + ".dll";
//		MessageBox.Show(CompilerParams.OutputAssembly);
		string[] code = new string[] { ReadString(reader) };
//		MessageBox.Show(code[i]);
	
	        CompilerResults compile = provider.CompileAssemblyFromSource(CompilerParams, code);
	        
	        if (compile.Errors.HasErrors)
	        {
	            string text = "Compile error: ";
	            foreach (CompilerError ce in compile.Errors)
	            {
	                text += "rn" + ce.ToString();
	            }
	            Console.WriteLine(text);
	            return;
	//            throw new Exception(text);
	        }
	        
//		MessageBox.Show(compile.CompiledAssembly.FullName);
		if(i==size-1)
	        	fstream = compile.CompiledAssembly.GetFiles()[0];
		else {
		        CompilerParams.ReferencedAssemblies.Add(compile.PathToAssembly);
		}
	}

	fstream.CopyTo(Console.OpenStandardOutput());
    }
}
}