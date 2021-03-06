package gnu.trove.stack;

import java.io.Serializable;

public abstract interface TFloatStack
  extends Serializable
{
  public abstract float getNoEntryValue();
  
  public abstract void push(float paramFloat);
  
  public abstract float pop();
  
  public abstract float peek();
  
  public abstract int size();
  
  public abstract void clear();
  
  public abstract float[] toArray();
  
  public abstract void toArray(float[] paramArrayOfFloat);
}
