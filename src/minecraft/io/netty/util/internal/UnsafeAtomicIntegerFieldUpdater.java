package io.netty.util.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import sun.misc.Unsafe;













final class UnsafeAtomicIntegerFieldUpdater<T>
  extends AtomicIntegerFieldUpdater<T>
{
  private final long offset;
  private final Unsafe unsafe;
  
  UnsafeAtomicIntegerFieldUpdater(Unsafe unsafe, Class<?> tClass, String fieldName)
    throws NoSuchFieldException
  {
    Field field = tClass.getDeclaredField(fieldName);
    if (!Modifier.isVolatile(field.getModifiers())) {
      throw new IllegalArgumentException("Must be volatile");
    }
    this.unsafe = unsafe;
    offset = unsafe.objectFieldOffset(field);
  }
  
  public boolean compareAndSet(T obj, int expect, int update)
  {
    return unsafe.compareAndSwapInt(obj, offset, expect, update);
  }
  
  public boolean weakCompareAndSet(T obj, int expect, int update)
  {
    return unsafe.compareAndSwapInt(obj, offset, expect, update);
  }
  
  public void set(T obj, int newValue)
  {
    unsafe.putIntVolatile(obj, offset, newValue);
  }
  
  public void lazySet(T obj, int newValue)
  {
    unsafe.putOrderedInt(obj, offset, newValue);
  }
  
  public int get(T obj)
  {
    return unsafe.getIntVolatile(obj, offset);
  }
}
