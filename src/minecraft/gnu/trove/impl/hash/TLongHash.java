package gnu.trove.impl.hash;

import gnu.trove.impl.Constants;
import gnu.trove.impl.HashFunctions;
import gnu.trove.procedure.TLongProcedure;
import java.util.Arrays;
















































public abstract class TLongHash
  extends TPrimitiveHash
{
  public transient long[] _set;
  protected long no_entry_value;
  
  public TLongHash()
  {
    no_entry_value = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
    
    if (no_entry_value != 0L) {
      Arrays.fill(_set, no_entry_value);
    }
  }
  







  public TLongHash(int initialCapacity)
  {
    super(initialCapacity);
    no_entry_value = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
    
    if (no_entry_value != 0L) {
      Arrays.fill(_set, no_entry_value);
    }
  }
  








  public TLongHash(int initialCapacity, float loadFactor)
  {
    super(initialCapacity, loadFactor);
    no_entry_value = Constants.DEFAULT_LONG_NO_ENTRY_VALUE;
    
    if (no_entry_value != 0L) {
      Arrays.fill(_set, no_entry_value);
    }
  }
  









  public TLongHash(int initialCapacity, float loadFactor, long no_entry_value)
  {
    super(initialCapacity, loadFactor);
    this.no_entry_value = no_entry_value;
    
    if (no_entry_value != 0L) {
      Arrays.fill(_set, no_entry_value);
    }
  }
  







  public long getNoEntryValue()
  {
    return no_entry_value;
  }
  









  protected int setUp(int initialCapacity)
  {
    int capacity = super.setUp(initialCapacity);
    _set = new long[capacity];
    return capacity;
  }
  






  public boolean contains(long val)
  {
    return index(val) >= 0;
  }
  







  public boolean forEach(TLongProcedure procedure)
  {
    byte[] states = _states;
    long[] set = _set;
    for (int i = set.length; i-- > 0;) {
      if ((states[i] == 1) && (!procedure.execute(set[i]))) {
        return false;
      }
    }
    return true;
  }
  





  public void removeAt(int index)
  {
    _set[index] = no_entry_value;
    super.removeAt(index);
  }
  








  protected int index(long val)
  {
    byte[] states = _states;
    long[] set = _set;
    int length = states.length;
    int hash = HashFunctions.hash(val) & 0x7FFFFFFF;
    int index = hash % length;
    
    if ((states[index] != 0) && ((states[index] == 2) || (set[index] != val)))
    {

      int probe = 1 + hash % (length - 2);
      do
      {
        index -= probe;
        if (index < 0) {
          index += length;
        }
      } while ((states[index] != 0) && ((states[index] == 2) || (set[index] != val)));
    }
    

    return states[index] == 0 ? -1 : index;
  }
  










  protected int insertionIndex(long val)
  {
    byte[] states = _states;
    long[] set = _set;
    int length = states.length;
    int hash = HashFunctions.hash(val) & 0x7FFFFFFF;
    int index = hash % length;
    
    if (states[index] == 0)
      return index;
    if ((states[index] == 1) && (set[index] == val)) {
      return -index - 1;
    }
    
    int probe = 1 + hash % (length - 2);
    












    if (states[index] != 2)
    {
      do
      {
        index -= probe;
        if (index < 0) {
          index += length;
        }
      } while ((states[index] == 1) && (set[index] != val));
    }
    



    if (states[index] == 2) {
      int firstRemoved = index;
      while ((states[index] != 0) && ((states[index] == 2) || (set[index] != val)))
      {
        index -= probe;
        if (index < 0) {
          index += length;
        }
      }
      return states[index] == 1 ? -index - 1 : firstRemoved;
    }
    
    return states[index] == 1 ? -index - 1 : index;
  }
}
