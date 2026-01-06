/**
 * <p>Title: java访问DBF文件的接口</p>
 * <p>Description: 这个类用于表示DBF文件中的写操作</p>
 * <p>Copyright: Copyright (c) 2004~2012~2012</p>
 * <p>Company: iihero.com</p>
 * @author : He Xiong
 * @version 1.1
 * fixed month in writeHeader
 */

package lsfusion.server.logics.form.stat.struct.export.plain.dbf;

import com.hexiong.jdbf.JDBFException;
import com.hexiong.jdbf.JDBField;
import lsfusion.server.physics.admin.Settings;

import java.io.*;
import java.util.Calendar;

/**
 * <p>Title: java访问DBF文件的接口</p>
 * <p>Description: 这个类用于表示DBF文件中的写操作</p>
 * <p>Copyright: Copyright (c) 2004~2012~2012</p>
 * <p>Company: iihero.com</p>
 * @author : He Xiong
 * @version 1.1
 */
public class DBFWriter {
  /**
   * 构造DBFWriter
   * @param s 文件名
   * @param ajdbfield 字段列表
   * @throws JDBFException 写文件出错时抛出异常
   */
  public DBFWriter(String s, JDBField ajdbfield[]) throws JDBFException {
    stream = null;
    recCount = 0;
    fields = null;
    fileName = null;
    dbfEncoding = null;
    fileName = s;
    try {
      init(new FileOutputStream(s), ajdbfield);
    }
    catch (FileNotFoundException filenotfoundexception) {
      throw new JDBFException(filenotfoundexception);
    }
  }
  /**
   * 构造函数
   * @param outputstream 输出流
   * @param ajdbfield 字段列表
   * @throws JDBFException 写文件出错时抛出异常
   */
  public DBFWriter(OutputStream outputstream, JDBField ajdbfield[]) throws
      JDBFException {
    stream = null;
    recCount = 0;
    fields = null;
    fileName = null;
    dbfEncoding = null;
    init(outputstream, ajdbfield);
  }
  /**
   * 构造函数
   * @param s 文件名
   * @param ajdbfield 字段列表
   * @param s1 字符集编码类型
   * @throws JDBFException
   */
  public DBFWriter(String s, JDBField ajdbfield[], String s1) throws
      JDBFException {
    stream = null;
    recCount = 0;
    fields = null;
    fileName = null;
    dbfEncoding = null;
    fileName = s;
    try {
      dbfEncoding = s1;
      init(new FileOutputStream(s), ajdbfield);
    }
    catch (FileNotFoundException filenotfoundexception) {
      throw new JDBFException(filenotfoundexception);
    }
  }

  /**
   * 初始化写操作
   * @param outputstream 输出流
   * @param ajdbfield 字段列表
   * @throws JDBFException 写操作失败时抛出
   */
  private void init(OutputStream outputstream, JDBField ajdbfield[]) throws
      JDBFException {
    fields = ajdbfield;
    try {
      stream = new BufferedOutputStream(outputstream);
      writeHeader();
      for (int i = 0; i < ajdbfield.length; i++) {
        writeFieldHeader(ajdbfield[i]);

      }
      stream.write(13);
      stream.flush();
    }
    catch (Exception exception) {
      throw new JDBFException(exception);
    }
  }

    private void writeHeader() throws IOException {
        byte[] abyte0 = new byte[16];
        abyte0[0] = 3;
        Calendar calendar = Calendar.getInstance();
        abyte0[1] = (byte) (calendar.get(Calendar.YEAR) - 1900);
        abyte0[2] = (byte) (calendar.get(Calendar.MONTH) + 1); //fixed month
        abyte0[3] = (byte) calendar.get(Calendar.DATE);
        abyte0[4] = 0;
        abyte0[5] = 0;
        abyte0[6] = 0;
        abyte0[7] = 0;
        int i = (fields.length + 1) * 32 + 1;
        abyte0[8] = (byte) (i % 256);
        abyte0[9] = (byte) (i / 256);
        int j = 1;
        for (JDBField field : fields) {
            j += field.getLength();
        }
        abyte0[10] = (byte) (j % 256);
        abyte0[11] = (byte) (j / 256);
        abyte0[12] = 0;
        abyte0[13] = 0;
        abyte0[14] = 0;
        abyte0[15] = 0;
        stream.write(abyte0, 0, abyte0.length);
        for (int l = 0; l < 16; l++) {
            abyte0[l] = l == 12 ? getLanguageDriverNameByte() : 0;
        }
        stream.write(abyte0, 0, abyte0.length);
    }

    //byte 29
    private byte getLanguageDriverNameByte() {
        if (Settings.get().isExportDBFLanguageDriverName() && dbfEncoding.equalsIgnoreCase("cp866")) {
            return 0x26;
        } else
            return 0;
    }

  /**
   * 写一个字段的元信息
   * @param jdbfield 字段内容
   * @throws IOException 写失败时抛出
   */
  private void writeFieldHeader(JDBField jdbfield) throws IOException {
    byte abyte0[] = new byte[16];
    String s = jdbfield.getName();
    int i = s.length();
    if (i > 10) {
      i = 10;
    }
    for (int j = 0; j < i; j++) {
      abyte0[j] = (byte) s.charAt(j);

    }
    for (int k = i; k <= 10; k++) {
      abyte0[k] = 0;

    }
    abyte0[11] = (byte) jdbfield.getType();
    abyte0[12] = 0;
    abyte0[13] = 0;
    abyte0[14] = 0;
    abyte0[15] = 0;
    stream.write(abyte0, 0, abyte0.length);
    for (int l = 0; l < 16; l++) {
      abyte0[l] = 0;

    }
    abyte0[0] = (byte) jdbfield.getLength();
    abyte0[1] = (byte) jdbfield.getDecimalCount();
    stream.write(abyte0, 0, abyte0.length);
  }

  /**
   * 写一条记录
   * @param aobj 以Object表示的记录值
   * @throws JDBFException 写操作失败时抛出,如果编码类型不支持，也抛出异常
   */
  public void addRecord(Object aobj[]) throws JDBFException {
    if (aobj.length != fields.length) {
      throw new JDBFException(
          "Error adding record: Wrong number of values. Expected " +
          fields.length + ", got " + aobj.length + ".");
    }
    int i = 0;
    for (int j = 0; j < fields.length; j++) {
      i += fields[j].getLength();

    }
    byte abyte0[] = new byte[i];
    int k = 0;
    for (int l = 0; l < fields.length; l++) {
      String s = fields[l].format(aobj[l]);
      byte abyte1[];
      try {
        if (dbfEncoding != null) {
          abyte1 = s.getBytes(dbfEncoding);
        }
        else {
          abyte1 = s.getBytes();
        }
      }
      catch (UnsupportedEncodingException unsupportedencodingexception) {
        throw new JDBFException(unsupportedencodingexception);
      }
      for (int i1 = 0; i1 < fields[l].getLength(); i1++) {
        abyte0[k + i1] = abyte1[i1];

      }
      k += fields[l].getLength();
    }

    try {
      stream.write(32);
      stream.write(abyte0, 0, abyte0.length);
      stream.flush();
    }
    catch (IOException ioexception) {
      throw new JDBFException(ioexception);
    }
    recCount++;
  }
  /**
   * 关闭文件写操作
   * @throws JDBFException 出现IO异常时抛出
   */
  public void close() throws JDBFException {
    try {
      stream.write(26);
      stream.close();
      RandomAccessFile randomaccessfile = new RandomAccessFile(fileName, "rw");
      randomaccessfile.seek(4L);
      byte abyte0[] = new byte[4];
      abyte0[0] = (byte) (recCount % 256);
      abyte0[1] = (byte) ( (recCount / 256) % 256);
      abyte0[2] = (byte) ( (recCount / 0x10000) % 256);
      abyte0[3] = (byte) ( (recCount / 0x1000000) % 256);
      randomaccessfile.write(abyte0, 0, abyte0.length);
      randomaccessfile.close();
    }
    catch (IOException ioexception) {
      throw new JDBFException(ioexception);
    }
  }

  /**
   * 输出流
   */
  private BufferedOutputStream stream;
  /**
   * 记录个数
   */
  private int recCount;
  /**
   * 字段列表
   */
  private JDBField fields[];
  /**
   * 文件名
   */
  private String fileName;
  /**
   * dbf文件的编码类型
   */
  private String dbfEncoding;
}