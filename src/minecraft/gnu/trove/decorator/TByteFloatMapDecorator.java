package gnu.trove.decorator;

import gnu.trove.iterator.TByteFloatIterator;
import gnu.trove.map.TByteFloatMap;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;












































public class TByteFloatMapDecorator
  extends AbstractMap<Byte, Float>
  implements Map<Byte, Float>, Externalizable, Cloneable
{
  protected TByteFloatMap _map;
  
  public TByteFloatMapDecorator() {}
  
  public TByteFloatMapDecorator(TByteFloatMap map)
  {
    _map = map;
  }
  





  public TByteFloatMap getMap()
  {
    return _map;
  }
  



  public Float put(Byte key, Float value)
  {
    byte k;
    


    byte k;
    

    if (key == null) {
      k = _map.getNoEntryKey();
    } else
      k = unwrapKey(key);
    float v;
    float v; if (value == null) {
      v = _map.getNoEntryValue();
    } else {
      v = unwrapValue(value);
    }
    float retval = _map.put(k, v);
    if (retval == _map.getNoEntryValue()) {
      return null;
    }
    return wrapValue(retval);
  }
  



  public Float get(Object key)
  {
    byte k;
    


    if (key != null) { byte k;
      if ((key instanceof Byte)) {
        k = unwrapKey(key);
      } else {
        return null;
      }
    } else {
      k = _map.getNoEntryKey();
    }
    float v = _map.get(k);
    


    if (v == _map.getNoEntryValue()) {
      return null;
    }
    return wrapValue(v);
  }
  




  public void clear()
  {
    _map.clear();
  }
  



  public Float remove(Object key)
  {
    byte k;
    


    if (key != null) { byte k;
      if ((key instanceof Byte)) {
        k = unwrapKey(key);
      } else {
        return null;
      }
    } else {
      k = _map.getNoEntryKey();
    }
    float v = _map.remove(k);
    


    if (v == _map.getNoEntryValue()) {
      return null;
    }
    return wrapValue(v);
  }
  






  public Set<Map.Entry<Byte, Float>> entrySet()
  {
    new AbstractSet() {
      public int size() {
        return _map.size();
      }
      
      public boolean isEmpty() {
        return TByteFloatMapDecorator.this.isEmpty();
      }
      
      public boolean contains(Object o) {
        if ((o instanceof Map.Entry)) {
          Object k = ((Map.Entry)o).getKey();
          Object v = ((Map.Entry)o).getValue();
          return (containsKey(k)) && (get(k).equals(v));
        }
        
        return false;
      }
      
      public Iterator<Map.Entry<Byte, Float>> iterator()
      {
        new Iterator() {
          private final TByteFloatIterator it = _map.iterator();
          
          public Map.Entry<Byte, Float> next() {
            it.advance();
            final Byte key = wrapKey(it.key());
            final Float v = wrapValue(it.value());
            new Map.Entry() {
              private Float val = v;
              
              public boolean equals(Object o) {
                return ((o instanceof Map.Entry)) && (((Map.Entry)o).getKey().equals(key)) && (((Map.Entry)o).getValue().equals(val));
              }
              

              public Byte getKey()
              {
                return key;
              }
              
              public Float getValue() {
                return val;
              }
              
              public int hashCode() {
                return key.hashCode() + val.hashCode();
              }
              
              public Float setValue(Float value) {
                val = value;
                return put(key, value);
              }
            };
          }
          
          public boolean hasNext() {
            return it.hasNext();
          }
          
          public void remove() {
            it.remove();
          }
        };
      }
      
      public boolean add(Map.Entry<Byte, Float> o) {
        throw new UnsupportedOperationException();
      }
      
      public boolean remove(Object o) {
        boolean modified = false;
        if (contains(o))
        {
          Byte key = (Byte)((Map.Entry)o).getKey();
          _map.remove(unwrapKey(key));
          modified = true;
        }
        return modified;
      }
      
      public boolean addAll(Collection<? extends Map.Entry<Byte, Float>> c) {
        throw new UnsupportedOperationException();
      }
      
      public void clear() {
        TByteFloatMapDecorator.this.clear();
      }
    };
  }
  






  public boolean containsValue(Object val)
  {
    return ((val instanceof Float)) && (_map.containsValue(unwrapValue(val)));
  }
  






  public boolean containsKey(Object key)
  {
    if (key == null) return _map.containsKey(_map.getNoEntryKey());
    return ((key instanceof Byte)) && (_map.containsKey(unwrapKey(key)));
  }
  





  public int size()
  {
    return _map.size();
  }
  





  public boolean isEmpty()
  {
    return size() == 0;
  }
  







  public void putAll(Map<? extends Byte, ? extends Float> map)
  {
    Iterator<? extends Map.Entry<? extends Byte, ? extends Float>> it = map.entrySet().iterator();
    
    for (int i = map.size(); i-- > 0;) {
      Map.Entry<? extends Byte, ? extends Float> e = (Map.Entry)it.next();
      put((Byte)e.getKey(), (Float)e.getValue());
    }
  }
  






  protected Byte wrapKey(byte k)
  {
    return Byte.valueOf(k);
  }
  






  protected byte unwrapKey(Object key)
  {
    return ((Byte)key).byteValue();
  }
  






  protected Float wrapValue(float k)
  {
    return Float.valueOf(k);
  }
  






  protected float unwrapValue(Object value)
  {
    return ((Float)value).floatValue();
  }
  



  public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException
  {
    in.readByte();
    

    _map = ((TByteFloatMap)in.readObject());
  }
  

  public void writeExternal(ObjectOutput out)
    throws IOException
  {
    out.writeByte(0);
    

    out.writeObject(_map);
  }
}
