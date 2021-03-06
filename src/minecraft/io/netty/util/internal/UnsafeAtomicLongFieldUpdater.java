package io.netty.util.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import sun.misc.Unsafe;













final class UnsafeAtomicLongFieldUpdater<T>
  extends AtomicLongFieldUpdater<T>
{
  private final long offset;
  private final Unsafe unsafe;
  
  UnsafeAtomicLongFieldUpdater(Unsafe unsafe, Class<?> tClass, String fieldName)
    throws NoSuchFieldException
  {
    Field field = tClass.getDeclaredField(fieldName);
    if (!Modifier.isVolatile(field.getModifiers())) {
      throw new IllegalArgumentException("Must be volatile");
    }
    this.unsafe = unsafe;
    offset = unsafe.objectFieldOffset(field);
  }
  
  public boolean compareAndSet(T obj, long expect, long update)
  {
    return unsafe.compareAndSwapLong(obj, offset, expect, update);
  }
  
  public boolean weakCompareAndSet(T obj, long expect, long update)
  {
    return unsafe.compareAndSwapLong(obj, offset, expect, update);
  }
  
  public void set(T obj, long newValue)
  {
    unsafe.putLongVolatile(obj, offset, newValue);
  }
  
  public void lazySet(T obj, long newValue)
  {
    unsafe.putOrderedLong(obj, offset, newValue);
  }
  
  public long get(T obj)
  {
    return unsafe.getLongVolatile(obj, offset);
  }
}
