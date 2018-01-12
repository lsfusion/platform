package lsfusion.server.logics.property.actions.external; /**
 * Copyright (C) 2011 DEHOF ingenieur+technik
 * Dipl.-Ing. (BA) Thomas Nenninger
 * http://www.dehof.de
 */

//заявлено, что в последней версии NumField2 уже не нужен, но на деле NumField вместо дробного числа записывает только целую часть

import org.xBaseJ.DBF;
import org.xBaseJ.Util;
import org.xBaseJ.fields.FloatField;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.xBaseJException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;


public class NumField2 extends NumField
{
 	//------------------------------------------------------------------------------------------------
 	//--- instance variables -------------------------------------------------------------------------
 	//------------------------------------------------------------------------------------------------
	private   static final long                  serialVersionUID  = 2011072901L;
	protected static       DecimalFormatSymbols  dfs               = new DecimalFormatSymbols();
	public static       char                  decimalSeparator  = dfs.getDecimalSeparator();
	
 	//------------------------------------------------------------------------------------------------
 	//--- constructors -------------------------------------------------------------------------------
 	//------------------------------------------------------------------------------------------------
	public NumField2() 
	{
		super();
	}
	
 	//------------------------------------------------------------------------------------------------
  public NumField2(String iName, int iLength, int inDecPosition) throws xBaseJException, IOException
	{
    super(iName, iLength, inDecPosition);
	}
	
 	//------------------------------------------------------------------------------------------------
	public NumField2(String iName, int iLength, int inDecPosition, ByteBuffer inBuffer) throws xBaseJException
	{
    super(iName, iLength, inDecPosition, inBuffer);
	}

	//------------------------------------------------------------------------------------------------
 	//--- overwritten from class 'Field' -------------------------------------------------------------
 	//------------------------------------------------------------------------------------------------
	public void put(double inDouble) throws xBaseJException
  {
    String inString = Double.toString(inDouble);
    
    StringBuilder formatString = new StringBuilder(getLength());
    for (int j=0; j<getLength(); j++)
  		{ formatString.append("#"); }
    if (getDecimalPositionCount()>0)
      { formatString.setCharAt(getDecimalPositionCount(),'.'); }
    
    DecimalFormat df = new DecimalFormat(formatString.toString());
    //df.setRoundingMode(RoundingMode.UNNECESSARY);
    inString = df.format(inDouble).trim();
    inString = inString.replace(decimalSeparator, '.');

    if (inString.length() > Length)
      { throw new xBaseJException("Field length too long; inDouble="+inString+" (maxLength="+Length+" / format="+formatString+")"); }

    int i = Math.min(inString.length(),Length);

    //-- fill database
		byte b[] = null;
    try 
    	{ b = inString.getBytes(DBF.encodedType); }
    catch(UnsupportedEncodingException uee)
      { b = inString.getBytes(); }

    for (i = 0; i < b.length; i++)
    	{ buffer[i] = b[i]; }

    byte fill;
    if (Util.fieldFilledWithSpaces())
      { fill =  (byte)' '; }
    else 
    	{ fill =  0; }

    for (i=inString.length(); i < Length; i++)
    	{ buffer[i] = fill; }
  }

	//------------------------------------------------------------------------------------------------
	public void put(float inFloat) throws xBaseJException
	{
		this.put((double)inFloat);
	}

	//------------------------------------------------------------------------------------------------
	public void put(String inString) throws xBaseJException
	{
		try
		{
		  this.put(Double.parseDouble(inString));
		}
		catch (NumberFormatException e)
		{
			throw new xBaseJException("Field-content not an number; inString="+inString+" : "+e.getLocalizedMessage());
		}
	}


	

 	//------------------------------------------------------------------------------------------------
 	//------------------------------------------------------------------------------------------------
 	//--- only for testing... ------------------------------------------------------------------------
 	//------------------------------------------------------------------------------------------------
 	//------------------------------------------------------------------------------------------------
	public static void main(String[] args)
	{
		try 
		{
			float floatValue = -76.123f;
			
			DBF db = new DBF("TEST_DB.dbf", true);
			NumField   numField   = new NumField("N", 7,3);
			FloatField floatField = new FloatField("F", 7,3);
			NumField2  numField2  = new NumField2("N2", 7,3);
			db.addField(numField);
			db.addField(floatField);
			db.addField(numField2);
			System.out.println("intial value before write               = "+floatValue);
			numField.put(floatValue);
			floatField.put(floatValue);
			numField2.put(floatValue);
			System.out.println("database-value (NumField) after write   = "+numField.get());
			System.out.println("database-value (FloatField) after write = "+floatField.get());
			System.out.println("database-value (NumField2) after write  = "+numField2.get());
			db.write();
			db.close();
		}
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}	
}


