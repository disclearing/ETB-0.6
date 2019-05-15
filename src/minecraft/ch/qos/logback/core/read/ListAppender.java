package ch.qos.logback.core.read;

import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;












public class ListAppender<E>
  extends AppenderBase<E>
{
  public ListAppender() {}
  
  public List<E> list = new ArrayList();
  
  protected void append(E e) {
    list.add(e);
  }
}