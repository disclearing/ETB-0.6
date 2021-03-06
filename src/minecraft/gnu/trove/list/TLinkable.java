package gnu.trove.list;

import java.io.Serializable;

public abstract interface TLinkable<T extends TLinkable>
  extends Serializable
{
  public abstract T getNext();
  
  public abstract T getPrevious();
  
  public abstract void setNext(T paramT);
  
  public abstract void setPrevious(T paramT);
}
