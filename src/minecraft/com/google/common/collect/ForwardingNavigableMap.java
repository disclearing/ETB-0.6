package com.google.common.collect;

import com.google.common.annotations.Beta;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;










































public abstract class ForwardingNavigableMap<K, V>
  extends ForwardingSortedMap<K, V>
  implements NavigableMap<K, V>
{
  protected ForwardingNavigableMap() {}
  
  protected abstract NavigableMap<K, V> delegate();
  
  public Map.Entry<K, V> lowerEntry(K key)
  {
    return delegate().lowerEntry(key);
  }
  




  protected Map.Entry<K, V> standardLowerEntry(K key)
  {
    return headMap(key, false).lastEntry();
  }
  
  public K lowerKey(K key)
  {
    return delegate().lowerKey(key);
  }
  




  protected K standardLowerKey(K key)
  {
    return Maps.keyOrNull(lowerEntry(key));
  }
  
  public Map.Entry<K, V> floorEntry(K key)
  {
    return delegate().floorEntry(key);
  }
  




  protected Map.Entry<K, V> standardFloorEntry(K key)
  {
    return headMap(key, true).lastEntry();
  }
  
  public K floorKey(K key)
  {
    return delegate().floorKey(key);
  }
  




  protected K standardFloorKey(K key)
  {
    return Maps.keyOrNull(floorEntry(key));
  }
  
  public Map.Entry<K, V> ceilingEntry(K key)
  {
    return delegate().ceilingEntry(key);
  }
  




  protected Map.Entry<K, V> standardCeilingEntry(K key)
  {
    return tailMap(key, true).firstEntry();
  }
  
  public K ceilingKey(K key)
  {
    return delegate().ceilingKey(key);
  }
  




  protected K standardCeilingKey(K key)
  {
    return Maps.keyOrNull(ceilingEntry(key));
  }
  
  public Map.Entry<K, V> higherEntry(K key)
  {
    return delegate().higherEntry(key);
  }
  




  protected Map.Entry<K, V> standardHigherEntry(K key)
  {
    return tailMap(key, false).firstEntry();
  }
  
  public K higherKey(K key)
  {
    return delegate().higherKey(key);
  }
  




  protected K standardHigherKey(K key)
  {
    return Maps.keyOrNull(higherEntry(key));
  }
  
  public Map.Entry<K, V> firstEntry()
  {
    return delegate().firstEntry();
  }
  




  protected Map.Entry<K, V> standardFirstEntry()
  {
    return (Map.Entry)Iterables.getFirst(entrySet(), null);
  }
  




  protected K standardFirstKey()
  {
    Map.Entry<K, V> entry = firstEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    }
    return entry.getKey();
  }
  

  public Map.Entry<K, V> lastEntry()
  {
    return delegate().lastEntry();
  }
  




  protected Map.Entry<K, V> standardLastEntry()
  {
    return (Map.Entry)Iterables.getFirst(descendingMap().entrySet(), null);
  }
  



  protected K standardLastKey()
  {
    Map.Entry<K, V> entry = lastEntry();
    if (entry == null) {
      throw new NoSuchElementException();
    }
    return entry.getKey();
  }
  

  public Map.Entry<K, V> pollFirstEntry()
  {
    return delegate().pollFirstEntry();
  }
  




  protected Map.Entry<K, V> standardPollFirstEntry()
  {
    return (Map.Entry)Iterators.pollNext(entrySet().iterator());
  }
  
  public Map.Entry<K, V> pollLastEntry()
  {
    return delegate().pollLastEntry();
  }
  




  protected Map.Entry<K, V> standardPollLastEntry()
  {
    return (Map.Entry)Iterators.pollNext(descendingMap().entrySet().iterator());
  }
  
  public NavigableMap<K, V> descendingMap()
  {
    return delegate().descendingMap();
  }
  






  @Beta
  protected class StandardDescendingMap
    extends Maps.DescendingMap<K, V>
  {
    public StandardDescendingMap() {}
    





    NavigableMap<K, V> forward()
    {
      return ForwardingNavigableMap.this;
    }
    
    protected Iterator<Map.Entry<K, V>> entryIterator()
    {
      new Iterator() {
        private Map.Entry<K, V> toRemove = null;
        private Map.Entry<K, V> nextOrNull = forward().lastEntry();
        
        public boolean hasNext()
        {
          return nextOrNull != null;
        }
        
        public Map.Entry<K, V> next()
        {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          try {
            return nextOrNull;
          } finally {
            toRemove = nextOrNull;
            nextOrNull = forward().lowerEntry(nextOrNull.getKey());
          }
        }
        
        public void remove()
        {
          CollectPreconditions.checkRemove(toRemove != null);
          forward().remove(toRemove.getKey());
          toRemove = null;
        }
      };
    }
  }
  
  public NavigableSet<K> navigableKeySet()
  {
    return delegate().navigableKeySet();
  }
  






  @Beta
  protected class StandardNavigableKeySet
    extends Maps.NavigableKeySet<K, V>
  {
    public StandardNavigableKeySet()
    {
      super();
    }
  }
  
  public NavigableSet<K> descendingKeySet()
  {
    return delegate().descendingKeySet();
  }
  






  @Beta
  protected NavigableSet<K> standardDescendingKeySet()
  {
    return descendingMap().navigableKeySet();
  }
  






  protected SortedMap<K, V> standardSubMap(K fromKey, K toKey)
  {
    return subMap(fromKey, true, toKey, false);
  }
  
  public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive)
  {
    return delegate().subMap(fromKey, fromInclusive, toKey, toInclusive);
  }
  
  public NavigableMap<K, V> headMap(K toKey, boolean inclusive)
  {
    return delegate().headMap(toKey, inclusive);
  }
  
  public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive)
  {
    return delegate().tailMap(fromKey, inclusive);
  }
  




  protected SortedMap<K, V> standardHeadMap(K toKey)
  {
    return headMap(toKey, false);
  }
  




  protected SortedMap<K, V> standardTailMap(K fromKey)
  {
    return tailMap(fromKey, true);
  }
}
