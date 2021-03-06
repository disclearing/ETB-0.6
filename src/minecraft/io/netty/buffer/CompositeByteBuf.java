package io.netty.buffer;

import io.netty.util.ResourceLeak;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.EmptyArrays;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;




















public class CompositeByteBuf
  extends AbstractReferenceCountedByteBuf
{
  private final ResourceLeak leak;
  private final ByteBufAllocator alloc;
  private final boolean direct;
  private final List<Component> components = new ArrayList();
  private final int maxNumComponents;
  private static final ByteBuffer FULL_BYTEBUFFER = (ByteBuffer)ByteBuffer.allocate(1).position(1);
  private boolean freed;
  
  public CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents)
  {
    super(Integer.MAX_VALUE);
    if (alloc == null) {
      throw new NullPointerException("alloc");
    }
    this.alloc = alloc;
    this.direct = direct;
    this.maxNumComponents = maxNumComponents;
    leak = leakDetector.open(this);
  }
  
  public CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, ByteBuf... buffers) {
    super(Integer.MAX_VALUE);
    if (alloc == null) {
      throw new NullPointerException("alloc");
    }
    if (maxNumComponents < 2) {
      throw new IllegalArgumentException("maxNumComponents: " + maxNumComponents + " (expected: >= 2)");
    }
    

    this.alloc = alloc;
    this.direct = direct;
    this.maxNumComponents = maxNumComponents;
    
    addComponents0(0, buffers);
    consolidateIfNeeded();
    setIndex(0, capacity());
    leak = leakDetector.open(this);
  }
  
  public CompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, Iterable<ByteBuf> buffers)
  {
    super(Integer.MAX_VALUE);
    if (alloc == null) {
      throw new NullPointerException("alloc");
    }
    if (maxNumComponents < 2) {
      throw new IllegalArgumentException("maxNumComponents: " + maxNumComponents + " (expected: >= 2)");
    }
    

    this.alloc = alloc;
    this.direct = direct;
    this.maxNumComponents = maxNumComponents;
    addComponents0(0, buffers);
    consolidateIfNeeded();
    setIndex(0, capacity());
    leak = leakDetector.open(this);
  }
  







  public CompositeByteBuf addComponent(ByteBuf buffer)
  {
    addComponent0(components.size(), buffer);
    consolidateIfNeeded();
    return this;
  }
  







  public CompositeByteBuf addComponents(ByteBuf... buffers)
  {
    addComponents0(components.size(), buffers);
    consolidateIfNeeded();
    return this;
  }
  







  public CompositeByteBuf addComponents(Iterable<ByteBuf> buffers)
  {
    addComponents0(components.size(), buffers);
    consolidateIfNeeded();
    return this;
  }
  








  public CompositeByteBuf addComponent(int cIndex, ByteBuf buffer)
  {
    addComponent0(cIndex, buffer);
    consolidateIfNeeded();
    return this;
  }
  
  private int addComponent0(int cIndex, ByteBuf buffer) {
    checkComponentIndex(cIndex);
    
    if (buffer == null) {
      throw new NullPointerException("buffer");
    }
    
    int readableBytes = buffer.readableBytes();
    if (readableBytes == 0) {
      return cIndex;
    }
    

    Component c = new Component(buffer.order(ByteOrder.BIG_ENDIAN).slice());
    if (cIndex == components.size()) {
      components.add(c);
      if (cIndex == 0) {
        endOffset = readableBytes;
      } else {
        Component prev = (Component)components.get(cIndex - 1);
        offset = endOffset;
        endOffset = (offset + readableBytes);
      }
    } else {
      components.add(cIndex, c);
      updateComponentOffsets(cIndex);
    }
    return cIndex;
  }
  








  public CompositeByteBuf addComponents(int cIndex, ByteBuf... buffers)
  {
    addComponents0(cIndex, buffers);
    consolidateIfNeeded();
    return this;
  }
  
  private int addComponents0(int cIndex, ByteBuf... buffers) {
    checkComponentIndex(cIndex);
    
    if (buffers == null) {
      throw new NullPointerException("buffers");
    }
    
    int readableBytes = 0;
    for (ByteBuf b : buffers) {
      if (b == null) {
        break;
      }
      readableBytes += b.readableBytes();
    }
    
    if (readableBytes == 0) {
      return cIndex;
    }
    

    for (ByteBuf b : buffers) {
      if (b == null) {
        break;
      }
      if (b.isReadable()) {
        cIndex = addComponent0(cIndex, b) + 1;
        int size = components.size();
        if (cIndex > size) {
          cIndex = size;
        }
      } else {
        b.release();
      }
    }
    return cIndex;
  }
  








  public CompositeByteBuf addComponents(int cIndex, Iterable<ByteBuf> buffers)
  {
    addComponents0(cIndex, buffers);
    consolidateIfNeeded();
    return this;
  }
  
  private int addComponents0(int cIndex, Iterable<ByteBuf> buffers) {
    if (buffers == null) {
      throw new NullPointerException("buffers");
    }
    
    if ((buffers instanceof ByteBuf))
    {
      return addComponent0(cIndex, (ByteBuf)buffers);
    }
    
    if (!(buffers instanceof Collection)) {
      List<ByteBuf> list = new ArrayList();
      for (ByteBuf b : buffers) {
        list.add(b);
      }
      buffers = list;
    }
    
    Collection<ByteBuf> col = (Collection)buffers;
    return addComponents0(cIndex, (ByteBuf[])col.toArray(new ByteBuf[col.size()]));
  }
  





  private void consolidateIfNeeded()
  {
    int numComponents = components.size();
    if (numComponents > maxNumComponents) {
      int capacity = components.get(numComponents - 1)).endOffset;
      
      ByteBuf consolidated = allocBuffer(capacity);
      

      for (int i = 0; i < numComponents; i++) {
        Component c = (Component)components.get(i);
        ByteBuf b = buf;
        consolidated.writeBytes(b);
        c.freeIfNecessary();
      }
      Component c = new Component(consolidated);
      endOffset = length;
      components.clear();
      components.add(c);
    }
  }
  
  private void checkComponentIndex(int cIndex) {
    ensureAccessible();
    if ((cIndex < 0) || (cIndex > components.size())) {
      throw new IndexOutOfBoundsException(String.format("cIndex: %d (expected: >= 0 && <= numComponents(%d))", new Object[] { Integer.valueOf(cIndex), Integer.valueOf(components.size()) }));
    }
  }
  

  private void checkComponentIndex(int cIndex, int numComponents)
  {
    ensureAccessible();
    if ((cIndex < 0) || (cIndex + numComponents > components.size())) {
      throw new IndexOutOfBoundsException(String.format("cIndex: %d, numComponents: %d (expected: cIndex >= 0 && cIndex + numComponents <= totalNumComponents(%d))", new Object[] { Integer.valueOf(cIndex), Integer.valueOf(numComponents), Integer.valueOf(components.size()) }));
    }
  }
  


  private void updateComponentOffsets(int cIndex)
  {
    int size = components.size();
    if (size <= cIndex) {
      return;
    }
    
    Component c = (Component)components.get(cIndex);
    if (cIndex == 0) {
      offset = 0;
      endOffset = length;
      cIndex++;
    }
    
    for (int i = cIndex; i < size; i++) {
      Component prev = (Component)components.get(i - 1);
      Component cur = (Component)components.get(i);
      offset = endOffset;
      endOffset = (offset + length);
    }
  }
  




  public CompositeByteBuf removeComponent(int cIndex)
  {
    checkComponentIndex(cIndex);
    ((Component)components.remove(cIndex)).freeIfNecessary();
    updateComponentOffsets(cIndex);
    return this;
  }
  





  public CompositeByteBuf removeComponents(int cIndex, int numComponents)
  {
    checkComponentIndex(cIndex, numComponents);
    
    List<Component> toRemove = components.subList(cIndex, cIndex + numComponents);
    for (Component c : toRemove) {
      c.freeIfNecessary();
    }
    toRemove.clear();
    
    updateComponentOffsets(cIndex);
    return this;
  }
  
  public Iterator<ByteBuf> iterator() {
    ensureAccessible();
    List<ByteBuf> list = new ArrayList(components.size());
    for (Component c : components) {
      list.add(buf);
    }
    return list.iterator();
  }
  


  public List<ByteBuf> decompose(int offset, int length)
  {
    checkIndex(offset, length);
    if (length == 0) {
      return Collections.emptyList();
    }
    
    int componentId = toComponentIndex(offset);
    List<ByteBuf> slice = new ArrayList(components.size());
    

    Component firstC = (Component)components.get(componentId);
    ByteBuf first = buf.duplicate();
    first.readerIndex(offset - offset);
    
    ByteBuf buf = first;
    int bytesToSlice = length;
    do {
      int readableBytes = buf.readableBytes();
      if (bytesToSlice <= readableBytes)
      {
        buf.writerIndex(buf.readerIndex() + bytesToSlice);
        slice.add(buf);
        break;
      }
      
      slice.add(buf);
      bytesToSlice -= readableBytes;
      componentId++;
      

      buf = components.get(componentId)).buf.duplicate();
    }
    while (bytesToSlice > 0);
    

    for (int i = 0; i < slice.size(); i++) {
      slice.set(i, ((ByteBuf)slice.get(i)).slice());
    }
    
    return slice;
  }
  
  public boolean isDirect()
  {
    int size = components.size();
    if (size == 0) {
      return false;
    }
    for (int i = 0; i < size; i++) {
      if (!components.get(i)).buf.isDirect()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean hasArray()
  {
    if (components.size() == 1) {
      return components.get(0)).buf.hasArray();
    }
    return false;
  }
  
  public byte[] array()
  {
    if (components.size() == 1) {
      return components.get(0)).buf.array();
    }
    throw new UnsupportedOperationException();
  }
  
  public int arrayOffset()
  {
    if (components.size() == 1) {
      return components.get(0)).buf.arrayOffset();
    }
    throw new UnsupportedOperationException();
  }
  
  public boolean hasMemoryAddress()
  {
    if (components.size() == 1) {
      return components.get(0)).buf.hasMemoryAddress();
    }
    return false;
  }
  
  public long memoryAddress()
  {
    if (components.size() == 1) {
      return components.get(0)).buf.memoryAddress();
    }
    throw new UnsupportedOperationException();
  }
  
  public int capacity()
  {
    if (components.isEmpty()) {
      return 0;
    }
    return components.get(components.size() - 1)).endOffset;
  }
  
  public CompositeByteBuf capacity(int newCapacity)
  {
    ensureAccessible();
    if ((newCapacity < 0) || (newCapacity > maxCapacity())) {
      throw new IllegalArgumentException("newCapacity: " + newCapacity);
    }
    
    int oldCapacity = capacity();
    if (newCapacity > oldCapacity) {
      int paddingLength = newCapacity - oldCapacity;
      
      int nComponents = components.size();
      if (nComponents < maxNumComponents) {
        ByteBuf padding = allocBuffer(paddingLength);
        padding.setIndex(0, paddingLength);
        addComponent0(components.size(), padding);
      } else {
        ByteBuf padding = allocBuffer(paddingLength);
        padding.setIndex(0, paddingLength);
        

        addComponent0(components.size(), padding);
        consolidateIfNeeded();
      }
    } else if (newCapacity < oldCapacity) {
      int bytesToTrim = oldCapacity - newCapacity;
      for (ListIterator<Component> i = components.listIterator(components.size()); i.hasPrevious();) {
        Component c = (Component)i.previous();
        if (bytesToTrim >= length) {
          bytesToTrim -= length;
          i.remove();

        }
        else
        {
          Component newC = new Component(buf.slice(0, length - bytesToTrim));
          offset = offset;
          endOffset = (offset + length);
          i.set(newC);
        }
      }
      
      if (readerIndex() > newCapacity) {
        setIndex(newCapacity, newCapacity);
      } else if (writerIndex() > newCapacity) {
        writerIndex(newCapacity);
      }
    }
    return this;
  }
  
  public ByteBufAllocator alloc()
  {
    return alloc;
  }
  
  public ByteOrder order()
  {
    return ByteOrder.BIG_ENDIAN;
  }
  


  public int numComponents()
  {
    return components.size();
  }
  


  public int maxNumComponents()
  {
    return maxNumComponents;
  }
  


  public int toComponentIndex(int offset)
  {
    checkIndex(offset);
    
    int low = 0; for (int high = components.size(); low <= high;) {
      int mid = low + high >>> 1;
      Component c = (Component)components.get(mid);
      if (offset >= endOffset) {
        low = mid + 1;
      } else if (offset < offset) {
        high = mid - 1;
      } else {
        return mid;
      }
    }
    
    throw new Error("should not reach here");
  }
  
  public int toByteIndex(int cIndex) {
    checkComponentIndex(cIndex);
    return components.get(cIndex)).offset;
  }
  
  public byte getByte(int index)
  {
    return _getByte(index);
  }
  
  protected byte _getByte(int index)
  {
    Component c = findComponent(index);
    return buf.getByte(index - offset);
  }
  
  protected short _getShort(int index)
  {
    Component c = findComponent(index);
    if (index + 2 <= endOffset)
      return buf.getShort(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (short)((_getByte(index) & 0xFF) << 8 | _getByte(index + 1) & 0xFF);
    }
    return (short)(_getByte(index) & 0xFF | (_getByte(index + 1) & 0xFF) << 8);
  }
  

  protected int _getUnsignedMedium(int index)
  {
    Component c = findComponent(index);
    if (index + 3 <= endOffset)
      return buf.getUnsignedMedium(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getShort(index) & 0xFFFF) << 8 | _getByte(index + 2) & 0xFF;
    }
    return _getShort(index) & 0xFFFF | (_getByte(index + 2) & 0xFF) << 16;
  }
  

  protected int _getInt(int index)
  {
    Component c = findComponent(index);
    if (index + 4 <= endOffset)
      return buf.getInt(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getShort(index) & 0xFFFF) << 16 | _getShort(index + 2) & 0xFFFF;
    }
    return _getShort(index) & 0xFFFF | (_getShort(index + 2) & 0xFFFF) << 16;
  }
  

  protected long _getLong(int index)
  {
    Component c = findComponent(index);
    if (index + 8 <= endOffset)
      return buf.getLong(index - offset);
    if (order() == ByteOrder.BIG_ENDIAN) {
      return (_getInt(index) & 0xFFFFFFFF) << 32 | _getInt(index + 4) & 0xFFFFFFFF;
    }
    return _getInt(index) & 0xFFFFFFFF | (_getInt(index + 4) & 0xFFFFFFFF) << 32;
  }
  

  public CompositeByteBuf getBytes(int index, byte[] dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex(index);
    while (length > 0) {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      s.getBytes(index - adjustment, dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuffer dst)
  {
    int limit = dst.limit();
    int length = dst.remaining();
    
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex(index);
    try {
      while (length > 0) {
        Component c = (Component)components.get(i);
        ByteBuf s = buf;
        int adjustment = offset;
        int localLength = Math.min(length, s.capacity() - (index - adjustment));
        dst.limit(dst.position() + localLength);
        s.getBytes(index - adjustment, dst);
        index += localLength;
        length -= localLength;
        i++;
      }
    } finally {
      dst.limit(limit);
    }
    return this;
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length)
  {
    checkDstIndex(index, length, dstIndex, dst.capacity());
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex(index);
    while (length > 0) {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      s.getBytes(index - adjustment, dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public int getBytes(int index, GatheringByteChannel out, int length)
    throws IOException
  {
    int count = nioBufferCount();
    if (count == 1) {
      return out.write(internalNioBuffer(index, length));
    }
    long writtenBytes = out.write(nioBuffers(index, length));
    if (writtenBytes > 2147483647L) {
      return Integer.MAX_VALUE;
    }
    return (int)writtenBytes;
  }
  

  public CompositeByteBuf getBytes(int index, OutputStream out, int length)
    throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex(index);
    while (length > 0) {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      s.getBytes(index - adjustment, out, localLength);
      index += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public CompositeByteBuf setByte(int index, int value)
  {
    Component c = findComponent(index);
    buf.setByte(index - offset, value);
    return this;
  }
  
  protected void _setByte(int index, int value)
  {
    setByte(index, value);
  }
  
  public CompositeByteBuf setShort(int index, int value)
  {
    return (CompositeByteBuf)super.setShort(index, value);
  }
  
  protected void _setShort(int index, int value)
  {
    Component c = findComponent(index);
    if (index + 2 <= endOffset) {
      buf.setShort(index - offset, value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setByte(index, (byte)(value >>> 8));
      _setByte(index + 1, (byte)value);
    } else {
      _setByte(index, (byte)value);
      _setByte(index + 1, (byte)(value >>> 8));
    }
  }
  
  public CompositeByteBuf setMedium(int index, int value)
  {
    return (CompositeByteBuf)super.setMedium(index, value);
  }
  
  protected void _setMedium(int index, int value)
  {
    Component c = findComponent(index);
    if (index + 3 <= endOffset) {
      buf.setMedium(index - offset, value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setShort(index, (short)(value >> 8));
      _setByte(index + 2, (byte)value);
    } else {
      _setShort(index, (short)value);
      _setByte(index + 2, (byte)(value >>> 16));
    }
  }
  
  public CompositeByteBuf setInt(int index, int value)
  {
    return (CompositeByteBuf)super.setInt(index, value);
  }
  
  protected void _setInt(int index, int value)
  {
    Component c = findComponent(index);
    if (index + 4 <= endOffset) {
      buf.setInt(index - offset, value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setShort(index, (short)(value >>> 16));
      _setShort(index + 2, (short)value);
    } else {
      _setShort(index, (short)value);
      _setShort(index + 2, (short)(value >>> 16));
    }
  }
  
  public CompositeByteBuf setLong(int index, long value)
  {
    return (CompositeByteBuf)super.setLong(index, value);
  }
  
  protected void _setLong(int index, long value)
  {
    Component c = findComponent(index);
    if (index + 8 <= endOffset) {
      buf.setLong(index - offset, value);
    } else if (order() == ByteOrder.BIG_ENDIAN) {
      _setInt(index, (int)(value >>> 32));
      _setInt(index + 4, (int)value);
    } else {
      _setInt(index, (int)value);
      _setInt(index + 4, (int)(value >>> 32));
    }
  }
  
  public CompositeByteBuf setBytes(int index, byte[] src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex(index);
    while (length > 0) {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      s.setBytes(index - adjustment, src, srcIndex, localLength);
      index += localLength;
      srcIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuffer src)
  {
    int limit = src.limit();
    int length = src.remaining();
    
    checkIndex(index, length);
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex(index);
    try {
      while (length > 0) {
        Component c = (Component)components.get(i);
        ByteBuf s = buf;
        int adjustment = offset;
        int localLength = Math.min(length, s.capacity() - (index - adjustment));
        src.limit(src.position() + localLength);
        s.setBytes(index - adjustment, src);
        index += localLength;
        length -= localLength;
        i++;
      }
    } finally {
      src.limit(limit);
    }
    return this;
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length)
  {
    checkSrcIndex(index, length, srcIndex, src.capacity());
    if (length == 0) {
      return this;
    }
    
    int i = toComponentIndex(index);
    while (length > 0) {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      s.setBytes(index - adjustment, src, srcIndex, localLength);
      index += localLength;
      srcIndex += localLength;
      length -= localLength;
      i++;
    }
    return this;
  }
  
  public int setBytes(int index, InputStream in, int length) throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return in.read(EmptyArrays.EMPTY_BYTES);
    }
    
    int i = toComponentIndex(index);
    int readBytes = 0;
    do
    {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      int localReadBytes = s.setBytes(index - adjustment, in, localLength);
      if (localReadBytes < 0) {
        if (readBytes != 0) break;
        return -1;
      }
      



      if (localReadBytes == localLength) {
        index += localLength;
        length -= localLength;
        readBytes += localLength;
        i++;
      } else {
        index += localReadBytes;
        length -= localReadBytes;
        readBytes += localReadBytes;
      }
    } while (length > 0);
    
    return readBytes;
  }
  
  public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException
  {
    checkIndex(index, length);
    if (length == 0) {
      return in.read(FULL_BYTEBUFFER);
    }
    
    int i = toComponentIndex(index);
    int readBytes = 0;
    do {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      int localReadBytes = s.setBytes(index - adjustment, in, localLength);
      
      if (localReadBytes == 0) {
        break;
      }
      
      if (localReadBytes < 0) {
        if (readBytes != 0) break;
        return -1;
      }
      



      if (localReadBytes == localLength) {
        index += localLength;
        length -= localLength;
        readBytes += localLength;
        i++;
      } else {
        index += localReadBytes;
        length -= localReadBytes;
        readBytes += localReadBytes;
      }
    } while (length > 0);
    
    return readBytes;
  }
  
  public ByteBuf copy(int index, int length)
  {
    checkIndex(index, length);
    ByteBuf dst = Unpooled.buffer(length);
    if (length != 0) {
      copyTo(index, length, toComponentIndex(index), dst);
    }
    return dst;
  }
  
  private void copyTo(int index, int length, int componentId, ByteBuf dst) {
    int dstIndex = 0;
    int i = componentId;
    
    while (length > 0) {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      s.getBytes(index - adjustment, dst, dstIndex, localLength);
      index += localLength;
      dstIndex += localLength;
      length -= localLength;
      i++;
    }
    
    dst.writerIndex(dst.capacity());
  }
  





  public ByteBuf component(int cIndex)
  {
    return internalComponent(cIndex).duplicate();
  }
  





  public ByteBuf componentAtOffset(int offset)
  {
    return internalComponentAtOffset(offset).duplicate();
  }
  





  public ByteBuf internalComponent(int cIndex)
  {
    checkComponentIndex(cIndex);
    return components.get(cIndex)).buf;
  }
  





  public ByteBuf internalComponentAtOffset(int offset)
  {
    return findComponentbuf;
  }
  
  private Component findComponent(int offset) {
    checkIndex(offset);
    
    int low = 0; for (int high = components.size(); low <= high;) {
      int mid = low + high >>> 1;
      Component c = (Component)components.get(mid);
      if (offset >= endOffset) {
        low = mid + 1;
      } else if (offset < offset) {
        high = mid - 1;
      } else {
        return c;
      }
    }
    
    throw new Error("should not reach here");
  }
  
  public int nioBufferCount()
  {
    if (components.size() == 1) {
      return components.get(0)).buf.nioBufferCount();
    }
    int count = 0;
    int componentsCount = components.size();
    for (int i = 0; i < componentsCount; i++) {
      Component c = (Component)components.get(i);
      count += buf.nioBufferCount();
    }
    return count;
  }
  

  public ByteBuffer internalNioBuffer(int index, int length)
  {
    if (components.size() == 1) {
      return components.get(0)).buf.internalNioBuffer(index, length);
    }
    throw new UnsupportedOperationException();
  }
  
  public ByteBuffer nioBuffer(int index, int length)
  {
    if (components.size() == 1) {
      ByteBuf buf = components.get(0)).buf;
      if (buf.nioBufferCount() == 1) {
        return components.get(0)).buf.nioBuffer(index, length);
      }
    }
    ByteBuffer merged = ByteBuffer.allocate(length).order(order());
    ByteBuffer[] buffers = nioBuffers(index, length);
    

    for (int i = 0; i < buffers.length; i++) {
      merged.put(buffers[i]);
    }
    
    merged.flip();
    return merged;
  }
  
  public ByteBuffer[] nioBuffers(int index, int length)
  {
    checkIndex(index, length);
    if (length == 0) {
      return EmptyArrays.EMPTY_BYTE_BUFFERS;
    }
    
    List<ByteBuffer> buffers = new ArrayList(components.size());
    int i = toComponentIndex(index);
    while (length > 0) {
      Component c = (Component)components.get(i);
      ByteBuf s = buf;
      int adjustment = offset;
      int localLength = Math.min(length, s.capacity() - (index - adjustment));
      switch (s.nioBufferCount()) {
      case 0: 
        throw new UnsupportedOperationException();
      case 1: 
        buffers.add(s.nioBuffer(index - adjustment, localLength));
        break;
      default: 
        Collections.addAll(buffers, s.nioBuffers(index - adjustment, localLength));
      }
      
      index += localLength;
      length -= localLength;
      i++;
    }
    
    return (ByteBuffer[])buffers.toArray(new ByteBuffer[buffers.size()]);
  }
  


  public CompositeByteBuf consolidate()
  {
    ensureAccessible();
    int numComponents = numComponents();
    if (numComponents <= 1) {
      return this;
    }
    
    Component last = (Component)components.get(numComponents - 1);
    int capacity = endOffset;
    ByteBuf consolidated = allocBuffer(capacity);
    
    for (int i = 0; i < numComponents; i++) {
      Component c = (Component)components.get(i);
      ByteBuf b = buf;
      consolidated.writeBytes(b);
      c.freeIfNecessary();
    }
    
    components.clear();
    components.add(new Component(consolidated));
    updateComponentOffsets(0);
    return this;
  }
  





  public CompositeByteBuf consolidate(int cIndex, int numComponents)
  {
    checkComponentIndex(cIndex, numComponents);
    if (numComponents <= 1) {
      return this;
    }
    
    int endCIndex = cIndex + numComponents;
    Component last = (Component)components.get(endCIndex - 1);
    int capacity = endOffset - components.get(cIndex)).offset;
    ByteBuf consolidated = allocBuffer(capacity);
    
    for (int i = cIndex; i < endCIndex; i++) {
      Component c = (Component)components.get(i);
      ByteBuf b = buf;
      consolidated.writeBytes(b);
      c.freeIfNecessary();
    }
    
    components.subList(cIndex + 1, endCIndex).clear();
    components.set(cIndex, new Component(consolidated));
    updateComponentOffsets(cIndex);
    return this;
  }
  


  public CompositeByteBuf discardReadComponents()
  {
    ensureAccessible();
    int readerIndex = readerIndex();
    if (readerIndex == 0) {
      return this;
    }
    

    int writerIndex = writerIndex();
    if ((readerIndex == writerIndex) && (writerIndex == capacity())) {
      for (Component c : components) {
        c.freeIfNecessary();
      }
      components.clear();
      setIndex(0, 0);
      adjustMarkers(readerIndex);
      return this;
    }
    

    int firstComponentId = toComponentIndex(readerIndex);
    for (int i = 0; i < firstComponentId; i++) {
      ((Component)components.get(i)).freeIfNecessary();
    }
    components.subList(0, firstComponentId).clear();
    

    Component first = (Component)components.get(0);
    int offset = offset;
    updateComponentOffsets(0);
    setIndex(readerIndex - offset, writerIndex - offset);
    adjustMarkers(offset);
    return this;
  }
  
  public CompositeByteBuf discardReadBytes()
  {
    ensureAccessible();
    int readerIndex = readerIndex();
    if (readerIndex == 0) {
      return this;
    }
    

    int writerIndex = writerIndex();
    if ((readerIndex == writerIndex) && (writerIndex == capacity())) {
      for (Component c : components) {
        c.freeIfNecessary();
      }
      components.clear();
      setIndex(0, 0);
      adjustMarkers(readerIndex);
      return this;
    }
    

    int firstComponentId = toComponentIndex(readerIndex);
    for (int i = 0; i < firstComponentId; i++) {
      ((Component)components.get(i)).freeIfNecessary();
    }
    components.subList(0, firstComponentId).clear();
    

    Component c = (Component)components.get(0);
    int adjustment = readerIndex - offset;
    if (adjustment == length)
    {
      components.remove(0);
    } else {
      Component newC = new Component(buf.slice(adjustment, length - adjustment));
      components.set(0, newC);
    }
    

    updateComponentOffsets(0);
    setIndex(0, writerIndex - readerIndex);
    adjustMarkers(readerIndex);
    return this;
  }
  
  private ByteBuf allocBuffer(int capacity) {
    if (direct) {
      return alloc().directBuffer(capacity);
    }
    return alloc().heapBuffer(capacity);
  }
  
  public String toString()
  {
    String result = super.toString();
    result = result.substring(0, result.length() - 1);
    return result + ", components=" + components.size() + ')';
  }
  
  private static final class Component {
    final ByteBuf buf;
    final int length;
    int offset;
    int endOffset;
    
    Component(ByteBuf buf) {
      this.buf = buf;
      length = buf.readableBytes();
    }
    
    void freeIfNecessary()
    {
      buf.release();
    }
  }
  
  public CompositeByteBuf readerIndex(int readerIndex)
  {
    return (CompositeByteBuf)super.readerIndex(readerIndex);
  }
  
  public CompositeByteBuf writerIndex(int writerIndex)
  {
    return (CompositeByteBuf)super.writerIndex(writerIndex);
  }
  
  public CompositeByteBuf setIndex(int readerIndex, int writerIndex)
  {
    return (CompositeByteBuf)super.setIndex(readerIndex, writerIndex);
  }
  
  public CompositeByteBuf clear()
  {
    return (CompositeByteBuf)super.clear();
  }
  
  public CompositeByteBuf markReaderIndex()
  {
    return (CompositeByteBuf)super.markReaderIndex();
  }
  
  public CompositeByteBuf resetReaderIndex()
  {
    return (CompositeByteBuf)super.resetReaderIndex();
  }
  
  public CompositeByteBuf markWriterIndex()
  {
    return (CompositeByteBuf)super.markWriterIndex();
  }
  
  public CompositeByteBuf resetWriterIndex()
  {
    return (CompositeByteBuf)super.resetWriterIndex();
  }
  
  public CompositeByteBuf ensureWritable(int minWritableBytes)
  {
    return (CompositeByteBuf)super.ensureWritable(minWritableBytes);
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst)
  {
    return (CompositeByteBuf)super.getBytes(index, dst);
  }
  
  public CompositeByteBuf getBytes(int index, ByteBuf dst, int length)
  {
    return (CompositeByteBuf)super.getBytes(index, dst, length);
  }
  
  public CompositeByteBuf getBytes(int index, byte[] dst)
  {
    return (CompositeByteBuf)super.getBytes(index, dst);
  }
  
  public CompositeByteBuf setBoolean(int index, boolean value)
  {
    return (CompositeByteBuf)super.setBoolean(index, value);
  }
  
  public CompositeByteBuf setChar(int index, int value)
  {
    return (CompositeByteBuf)super.setChar(index, value);
  }
  
  public CompositeByteBuf setFloat(int index, float value)
  {
    return (CompositeByteBuf)super.setFloat(index, value);
  }
  
  public CompositeByteBuf setDouble(int index, double value)
  {
    return (CompositeByteBuf)super.setDouble(index, value);
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src)
  {
    return (CompositeByteBuf)super.setBytes(index, src);
  }
  
  public CompositeByteBuf setBytes(int index, ByteBuf src, int length)
  {
    return (CompositeByteBuf)super.setBytes(index, src, length);
  }
  
  public CompositeByteBuf setBytes(int index, byte[] src)
  {
    return (CompositeByteBuf)super.setBytes(index, src);
  }
  
  public CompositeByteBuf setZero(int index, int length)
  {
    return (CompositeByteBuf)super.setZero(index, length);
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst)
  {
    return (CompositeByteBuf)super.readBytes(dst);
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst, int length)
  {
    return (CompositeByteBuf)super.readBytes(dst, length);
  }
  
  public CompositeByteBuf readBytes(ByteBuf dst, int dstIndex, int length)
  {
    return (CompositeByteBuf)super.readBytes(dst, dstIndex, length);
  }
  
  public CompositeByteBuf readBytes(byte[] dst)
  {
    return (CompositeByteBuf)super.readBytes(dst);
  }
  
  public CompositeByteBuf readBytes(byte[] dst, int dstIndex, int length)
  {
    return (CompositeByteBuf)super.readBytes(dst, dstIndex, length);
  }
  
  public CompositeByteBuf readBytes(ByteBuffer dst)
  {
    return (CompositeByteBuf)super.readBytes(dst);
  }
  
  public CompositeByteBuf readBytes(OutputStream out, int length) throws IOException
  {
    return (CompositeByteBuf)super.readBytes(out, length);
  }
  
  public CompositeByteBuf skipBytes(int length)
  {
    return (CompositeByteBuf)super.skipBytes(length);
  }
  
  public CompositeByteBuf writeBoolean(boolean value)
  {
    return (CompositeByteBuf)super.writeBoolean(value);
  }
  
  public CompositeByteBuf writeByte(int value)
  {
    return (CompositeByteBuf)super.writeByte(value);
  }
  
  public CompositeByteBuf writeShort(int value)
  {
    return (CompositeByteBuf)super.writeShort(value);
  }
  
  public CompositeByteBuf writeMedium(int value)
  {
    return (CompositeByteBuf)super.writeMedium(value);
  }
  
  public CompositeByteBuf writeInt(int value)
  {
    return (CompositeByteBuf)super.writeInt(value);
  }
  
  public CompositeByteBuf writeLong(long value)
  {
    return (CompositeByteBuf)super.writeLong(value);
  }
  
  public CompositeByteBuf writeChar(int value)
  {
    return (CompositeByteBuf)super.writeChar(value);
  }
  
  public CompositeByteBuf writeFloat(float value)
  {
    return (CompositeByteBuf)super.writeFloat(value);
  }
  
  public CompositeByteBuf writeDouble(double value)
  {
    return (CompositeByteBuf)super.writeDouble(value);
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src)
  {
    return (CompositeByteBuf)super.writeBytes(src);
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src, int length)
  {
    return (CompositeByteBuf)super.writeBytes(src, length);
  }
  
  public CompositeByteBuf writeBytes(ByteBuf src, int srcIndex, int length)
  {
    return (CompositeByteBuf)super.writeBytes(src, srcIndex, length);
  }
  
  public CompositeByteBuf writeBytes(byte[] src)
  {
    return (CompositeByteBuf)super.writeBytes(src);
  }
  
  public CompositeByteBuf writeBytes(byte[] src, int srcIndex, int length)
  {
    return (CompositeByteBuf)super.writeBytes(src, srcIndex, length);
  }
  
  public CompositeByteBuf writeBytes(ByteBuffer src)
  {
    return (CompositeByteBuf)super.writeBytes(src);
  }
  
  public CompositeByteBuf writeZero(int length)
  {
    return (CompositeByteBuf)super.writeZero(length);
  }
  
  public CompositeByteBuf retain(int increment)
  {
    return (CompositeByteBuf)super.retain(increment);
  }
  
  public CompositeByteBuf retain()
  {
    return (CompositeByteBuf)super.retain();
  }
  
  public ByteBuffer[] nioBuffers()
  {
    return nioBuffers(readerIndex(), readableBytes());
  }
  
  public CompositeByteBuf discardSomeReadBytes()
  {
    return discardReadComponents();
  }
  
  protected void deallocate()
  {
    if (freed) {
      return;
    }
    
    freed = true;
    int size = components.size();
    

    for (int i = 0; i < size; i++) {
      ((Component)components.get(i)).freeIfNecessary();
    }
    
    if (leak != null) {
      leak.close();
    }
  }
  
  public ByteBuf unwrap()
  {
    return null;
  }
}
