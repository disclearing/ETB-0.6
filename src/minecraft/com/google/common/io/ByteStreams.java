package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;



































@Beta
public final class ByteStreams
{
  private static final int BUF_SIZE = 4096;
  
  private ByteStreams() {}
  
  @Deprecated
  public static InputSupplier<ByteArrayInputStream> newInputStreamSupplier(byte[] b)
  {
    return asInputSupplier(ByteSource.wrap(b));
  }
  











  @Deprecated
  public static InputSupplier<ByteArrayInputStream> newInputStreamSupplier(byte[] b, int off, int len)
  {
    return asInputSupplier(ByteSource.wrap(b).slice(off, len));
  }
  








  @Deprecated
  public static void write(byte[] from, OutputSupplier<? extends OutputStream> to)
    throws IOException
  {
    asByteSink(to).write(from);
  }
  










  @Deprecated
  public static long copy(InputSupplier<? extends InputStream> from, OutputSupplier<? extends OutputStream> to)
    throws IOException
  {
    return asByteSource(from).copyTo(asByteSink(to));
  }
  











  @Deprecated
  public static long copy(InputSupplier<? extends InputStream> from, OutputStream to)
    throws IOException
  {
    return asByteSource(from).copyTo(to);
  }
  












  @Deprecated
  public static long copy(InputStream from, OutputSupplier<? extends OutputStream> to)
    throws IOException
  {
    return asByteSink(to).writeFrom(from);
  }
  








  public static long copy(InputStream from, OutputStream to)
    throws IOException
  {
    Preconditions.checkNotNull(from);
    Preconditions.checkNotNull(to);
    byte[] buf = new byte['က'];
    long total = 0L;
    for (;;) {
      int r = from.read(buf);
      if (r == -1) {
        break;
      }
      to.write(buf, 0, r);
      total += r;
    }
    return total;
  }
  








  public static long copy(ReadableByteChannel from, WritableByteChannel to)
    throws IOException
  {
    Preconditions.checkNotNull(from);
    Preconditions.checkNotNull(to);
    ByteBuffer buf = ByteBuffer.allocate(4096);
    long total = 0L;
    while (from.read(buf) != -1) {
      buf.flip();
      while (buf.hasRemaining()) {
        total += to.write(buf);
      }
      buf.clear();
    }
    return total;
  }
  






  public static byte[] toByteArray(InputStream in)
    throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(in, out);
    return out.toByteArray();
  }
  





  static byte[] toByteArray(InputStream in, int expectedSize)
    throws IOException
  {
    byte[] bytes = new byte[expectedSize];
    int remaining = expectedSize;
    
    while (remaining > 0) {
      int off = expectedSize - remaining;
      int read = in.read(bytes, off, remaining);
      if (read == -1)
      {

        return Arrays.copyOf(bytes, off);
      }
      remaining -= read;
    }
    

    int b = in.read();
    if (b == -1) {
      return bytes;
    }
    

    FastByteArrayOutputStream out = new FastByteArrayOutputStream(null);
    out.write(b);
    copy(in, out);
    
    byte[] result = new byte[bytes.length + out.size()];
    System.arraycopy(bytes, 0, result, 0, bytes.length);
    out.writeTo(result, bytes.length);
    return result;
  }
  


  private static final class FastByteArrayOutputStream
    extends ByteArrayOutputStream
  {
    private FastByteArrayOutputStream() {}
    

    void writeTo(byte[] b, int off)
    {
      System.arraycopy(buf, 0, b, off, count);
    }
  }
  







  @Deprecated
  public static byte[] toByteArray(InputSupplier<? extends InputStream> supplier)
    throws IOException
  {
    return asByteSource(supplier).read();
  }
  



  public static ByteArrayDataInput newDataInput(byte[] bytes)
  {
    return newDataInput(new ByteArrayInputStream(bytes));
  }
  






  public static ByteArrayDataInput newDataInput(byte[] bytes, int start)
  {
    Preconditions.checkPositionIndex(start, bytes.length);
    return newDataInput(new ByteArrayInputStream(bytes, start, bytes.length - start));
  }
  








  public static ByteArrayDataInput newDataInput(ByteArrayInputStream byteArrayInputStream)
  {
    return new ByteArrayDataInputStream((ByteArrayInputStream)Preconditions.checkNotNull(byteArrayInputStream));
  }
  
  private static class ByteArrayDataInputStream implements ByteArrayDataInput {
    final DataInput input;
    
    ByteArrayDataInputStream(ByteArrayInputStream byteArrayInputStream) {
      input = new DataInputStream(byteArrayInputStream);
    }
    
    public void readFully(byte[] b) {
      try {
        input.readFully(b);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public void readFully(byte[] b, int off, int len) {
      try {
        input.readFully(b, off, len);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public int skipBytes(int n) {
      try {
        return input.skipBytes(n);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public boolean readBoolean() {
      try {
        return input.readBoolean();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public byte readByte() {
      try {
        return input.readByte();
      } catch (EOFException e) {
        throw new IllegalStateException(e);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public int readUnsignedByte() {
      try {
        return input.readUnsignedByte();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public short readShort() {
      try {
        return input.readShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public int readUnsignedShort() {
      try {
        return input.readUnsignedShort();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public char readChar() {
      try {
        return input.readChar();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public int readInt() {
      try {
        return input.readInt();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public long readLong() {
      try {
        return input.readLong();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public float readFloat() {
      try {
        return input.readFloat();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public double readDouble() {
      try {
        return input.readDouble();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public String readLine() {
      try {
        return input.readLine();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    
    public String readUTF() {
      try {
        return input.readUTF();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
  


  public static ByteArrayDataOutput newDataOutput()
  {
    return newDataOutput(new ByteArrayOutputStream());
  }
  





  public static ByteArrayDataOutput newDataOutput(int size)
  {
    Preconditions.checkArgument(size >= 0, "Invalid size: %s", new Object[] { Integer.valueOf(size) });
    return newDataOutput(new ByteArrayOutputStream(size));
  }
  














  public static ByteArrayDataOutput newDataOutput(ByteArrayOutputStream byteArrayOutputSteam)
  {
    return new ByteArrayDataOutputStream((ByteArrayOutputStream)Preconditions.checkNotNull(byteArrayOutputSteam));
  }
  
  private static class ByteArrayDataOutputStream
    implements ByteArrayDataOutput
  {
    final DataOutput output;
    final ByteArrayOutputStream byteArrayOutputSteam;
    
    ByteArrayDataOutputStream(ByteArrayOutputStream byteArrayOutputSteam)
    {
      this.byteArrayOutputSteam = byteArrayOutputSteam;
      output = new DataOutputStream(byteArrayOutputSteam);
    }
    
    public void write(int b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void write(byte[] b) {
      try {
        output.write(b);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void write(byte[] b, int off, int len) {
      try {
        output.write(b, off, len);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeBoolean(boolean v) {
      try {
        output.writeBoolean(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeByte(int v) {
      try {
        output.writeByte(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeBytes(String s) {
      try {
        output.writeBytes(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeChar(int v) {
      try {
        output.writeChar(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeChars(String s) {
      try {
        output.writeChars(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeDouble(double v) {
      try {
        output.writeDouble(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeFloat(float v) {
      try {
        output.writeFloat(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeInt(int v) {
      try {
        output.writeInt(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeLong(long v) {
      try {
        output.writeLong(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeShort(int v) {
      try {
        output.writeShort(v);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public void writeUTF(String s) {
      try {
        output.writeUTF(s);
      } catch (IOException impossible) {
        throw new AssertionError(impossible);
      }
    }
    
    public byte[] toByteArray() {
      return byteArrayOutputSteam.toByteArray();
    }
  }
  
  private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream()
  {
    public void write(int b) {}
    

    public void write(byte[] b)
    {
      Preconditions.checkNotNull(b);
    }
    
    public void write(byte[] b, int off, int len) {
      Preconditions.checkNotNull(b);
    }
    
    public String toString()
    {
      return "ByteStreams.nullOutputStream()";
    }
  };
  




  public static OutputStream nullOutputStream()
  {
    return NULL_OUTPUT_STREAM;
  }
  








  public static InputStream limit(InputStream in, long limit)
  {
    return new LimitedInputStream(in, limit);
  }
  
  private static final class LimitedInputStream extends FilterInputStream
  {
    private long left;
    private long mark = -1L;
    
    LimitedInputStream(InputStream in, long limit) {
      super();
      Preconditions.checkNotNull(in);
      Preconditions.checkArgument(limit >= 0L, "limit must be non-negative");
      left = limit;
    }
    
    public int available() throws IOException {
      return (int)Math.min(in.available(), left);
    }
    
    public synchronized void mark(int readLimit)
    {
      in.mark(readLimit);
      mark = left;
    }
    
    public int read() throws IOException {
      if (left == 0L) {
        return -1;
      }
      
      int result = in.read();
      if (result != -1) {
        left -= 1L;
      }
      return result;
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
      if (left == 0L) {
        return -1;
      }
      
      len = (int)Math.min(len, left);
      int result = in.read(b, off, len);
      if (result != -1) {
        left -= result;
      }
      return result;
    }
    
    public synchronized void reset() throws IOException {
      if (!in.markSupported()) {
        throw new IOException("Mark not supported");
      }
      if (mark == -1L) {
        throw new IOException("Mark not set");
      }
      
      in.reset();
      left = mark;
    }
    
    public long skip(long n) throws IOException {
      n = Math.min(n, left);
      long skipped = in.skip(n);
      left -= skipped;
      return skipped;
    }
  }
  





  @Deprecated
  public static long length(InputSupplier<? extends InputStream> supplier)
    throws IOException
  {
    return asByteSource(supplier).size();
  }
  






  @Deprecated
  public static boolean equal(InputSupplier<? extends InputStream> supplier1, InputSupplier<? extends InputStream> supplier2)
    throws IOException
  {
    return asByteSource(supplier1).contentEquals(asByteSource(supplier2));
  }
  









  public static void readFully(InputStream in, byte[] b)
    throws IOException
  {
    readFully(in, b, 0, b.length);
  }
  













  public static void readFully(InputStream in, byte[] b, int off, int len)
    throws IOException
  {
    int read = read(in, b, off, len);
    if (read != len) {
      throw new EOFException("reached end of stream after reading " + read + " bytes; " + len + " bytes expected");
    }
  }
  











  public static void skipFully(InputStream in, long n)
    throws IOException
  {
    long toSkip = n;
    while (n > 0L) {
      long amt = in.skip(n);
      if (amt == 0L)
      {
        if (in.read() == -1) {
          long skipped = toSkip - n;
          throw new EOFException("reached end of stream after skipping " + skipped + " bytes; " + toSkip + " bytes expected");
        }
        
        n -= 1L;
      } else {
        n -= amt;
      }
    }
  }
  










  @Deprecated
  public static <T> T readBytes(InputSupplier<? extends InputStream> supplier, ByteProcessor<T> processor)
    throws IOException
  {
    Preconditions.checkNotNull(supplier);
    Preconditions.checkNotNull(processor);
    
    Closer closer = Closer.create();
    try {
      InputStream in = (InputStream)closer.register((Closeable)supplier.getInput());
      return readBytes(in, processor);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }
  








  public static <T> T readBytes(InputStream input, ByteProcessor<T> processor)
    throws IOException
  {
    Preconditions.checkNotNull(input);
    Preconditions.checkNotNull(processor);
    
    byte[] buf = new byte['က'];
    int read;
    do {
      read = input.read(buf);
    } while ((read != -1) && (processor.processBytes(buf, 0, read)));
    return processor.getResult();
  }
  












  @Deprecated
  public static HashCode hash(InputSupplier<? extends InputStream> supplier, HashFunction hashFunction)
    throws IOException
  {
    return asByteSource(supplier).hash(hashFunction);
  }
  























  public static int read(InputStream in, byte[] b, int off, int len)
    throws IOException
  {
    Preconditions.checkNotNull(in);
    Preconditions.checkNotNull(b);
    if (len < 0) {
      throw new IndexOutOfBoundsException("len is negative");
    }
    int total = 0;
    while (total < len) {
      int result = in.read(b, off + total, len - total);
      if (result == -1) {
        break;
      }
      total += result;
    }
    return total;
  }
  















  @Deprecated
  public static InputSupplier<InputStream> slice(InputSupplier<? extends InputStream> supplier, long offset, long length)
  {
    return asInputSupplier(asByteSource(supplier).slice(offset, length));
  }
  

















  @Deprecated
  public static InputSupplier<InputStream> join(Iterable<? extends InputSupplier<? extends InputStream>> suppliers)
  {
    Preconditions.checkNotNull(suppliers);
    Iterable<ByteSource> sources = Iterables.transform(suppliers, new Function()
    {
      public ByteSource apply(InputSupplier<? extends InputStream> input)
      {
        return ByteStreams.asByteSource(input);
      }
    });
    return asInputSupplier(ByteSource.concat(sources));
  }
  







  @Deprecated
  public static InputSupplier<InputStream> join(InputSupplier<? extends InputStream>... suppliers)
  {
    return join(Arrays.asList(suppliers));
  }
  















  @Deprecated
  public static ByteSource asByteSource(InputSupplier<? extends InputStream> supplier)
  {
    Preconditions.checkNotNull(supplier);
    new ByteSource()
    {
      public InputStream openStream() throws IOException {
        return (InputStream)val$supplier.getInput();
      }
      
      public String toString()
      {
        return "ByteStreams.asByteSource(" + val$supplier + ")";
      }
    };
  }
  













  @Deprecated
  public static ByteSink asByteSink(OutputSupplier<? extends OutputStream> supplier)
  {
    Preconditions.checkNotNull(supplier);
    new ByteSink()
    {
      public OutputStream openStream() throws IOException {
        return (OutputStream)val$supplier.getOutput();
      }
      
      public String toString()
      {
        return "ByteStreams.asByteSink(" + val$supplier + ")";
      }
    };
  }
  

  static <S extends InputStream> InputSupplier<S> asInputSupplier(ByteSource source)
  {
    return (InputSupplier)Preconditions.checkNotNull(source);
  }
  

  static <S extends OutputStream> OutputSupplier<S> asOutputSupplier(ByteSink sink)
  {
    return (OutputSupplier)Preconditions.checkNotNull(sink);
  }
}
