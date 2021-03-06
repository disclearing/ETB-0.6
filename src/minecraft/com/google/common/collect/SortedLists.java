package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.Nullable;


































@GwtCompatible
@Beta
final class SortedLists
{
  private SortedLists() {}
  
  public static abstract enum KeyPresentBehavior
  {
    ANY_PRESENT, 
    








    LAST_PRESENT, 
    






















    FIRST_PRESENT, 
    
























    FIRST_AFTER, 
    









    LAST_BEFORE;
    




    private KeyPresentBehavior() {}
    




    abstract <E> int resultIndex(Comparator<? super E> paramComparator, E paramE, List<? extends E> paramList, int paramInt);
  }
  



  public static abstract enum KeyAbsentBehavior
  {
    NEXT_LOWER, 
    








    NEXT_HIGHER, 
    
















    INVERTED_INSERTION_INDEX;
    




    private KeyAbsentBehavior() {}
    



    abstract int resultIndex(int paramInt);
  }
  



  public static <E extends Comparable> int binarySearch(List<? extends E> list, E e, KeyPresentBehavior presentBehavior, KeyAbsentBehavior absentBehavior)
  {
    Preconditions.checkNotNull(e);
    return binarySearch(list, Preconditions.checkNotNull(e), Ordering.natural(), presentBehavior, absentBehavior);
  }
  








  public static <E, K extends Comparable> int binarySearch(List<E> list, Function<? super E, K> keyFunction, @Nullable K key, KeyPresentBehavior presentBehavior, KeyAbsentBehavior absentBehavior)
  {
    return binarySearch(list, keyFunction, key, Ordering.natural(), presentBehavior, absentBehavior);
  }
  


















  public static <E, K> int binarySearch(List<E> list, Function<? super E, K> keyFunction, @Nullable K key, Comparator<? super K> keyComparator, KeyPresentBehavior presentBehavior, KeyAbsentBehavior absentBehavior)
  {
    return binarySearch(Lists.transform(list, keyFunction), key, keyComparator, presentBehavior, absentBehavior);
  }
  

























  public static <E> int binarySearch(List<? extends E> list, @Nullable E key, Comparator<? super E> comparator, KeyPresentBehavior presentBehavior, KeyAbsentBehavior absentBehavior)
  {
    Preconditions.checkNotNull(comparator);
    Preconditions.checkNotNull(list);
    Preconditions.checkNotNull(presentBehavior);
    Preconditions.checkNotNull(absentBehavior);
    if (!(list instanceof RandomAccess)) {
      list = Lists.newArrayList(list);
    }
    

    int lower = 0;
    int upper = list.size() - 1;
    
    while (lower <= upper) {
      int middle = lower + upper >>> 1;
      int c = comparator.compare(key, list.get(middle));
      if (c < 0) {
        upper = middle - 1;
      } else if (c > 0) {
        lower = middle + 1;
      } else {
        return lower + presentBehavior.resultIndex(comparator, key, list.subList(lower, upper + 1), middle - lower);
      }
    }
    
    return absentBehavior.resultIndex(lower);
  }
}
