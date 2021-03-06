package com.google.common.cache;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.util.Map.Entry;
import javax.annotation.Nullable;

























@Beta
@GwtCompatible
public final class RemovalNotification<K, V>
  implements Map.Entry<K, V>
{
  @Nullable
  private final K key;
  @Nullable
  private final V value;
  private final RemovalCause cause;
  private static final long serialVersionUID = 0L;
  
  RemovalNotification(@Nullable K key, @Nullable V value, RemovalCause cause)
  {
    this.key = key;
    this.value = value;
    this.cause = ((RemovalCause)Preconditions.checkNotNull(cause));
  }
  


  public RemovalCause getCause()
  {
    return cause;
  }
  



  public boolean wasEvicted()
  {
    return cause.wasEvicted();
  }
  
  @Nullable
  public K getKey() { return key; }
  
  @Nullable
  public V getValue() {
    return value;
  }
  
  public final V setValue(V value) {
    throw new UnsupportedOperationException();
  }
  
  public boolean equals(@Nullable Object object) {
    if ((object instanceof Map.Entry)) {
      Map.Entry<?, ?> that = (Map.Entry)object;
      return (Objects.equal(getKey(), that.getKey())) && (Objects.equal(getValue(), that.getValue()));
    }
    
    return false;
  }
  
  public int hashCode() {
    K k = getKey();
    V v = getValue();
    return (k == null ? 0 : k.hashCode()) ^ (v == null ? 0 : v.hashCode());
  }
  


  public String toString()
  {
    return getKey() + "=" + getValue();
  }
}
