package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;






























@Beta
public final class ImmutableRangeSet<C extends Comparable>
  extends AbstractRangeSet<C>
  implements Serializable
{
  private static final ImmutableRangeSet<Comparable<?>> EMPTY = new ImmutableRangeSet(ImmutableList.of());
  

  private static final ImmutableRangeSet<Comparable<?>> ALL = new ImmutableRangeSet(ImmutableList.of(Range.all()));
  
  private final transient ImmutableList<Range<C>> ranges;
  
  private transient ImmutableRangeSet<C> complement;
  
  public static <C extends Comparable> ImmutableRangeSet<C> of()
  {
    return EMPTY;
  }
  



  static <C extends Comparable> ImmutableRangeSet<C> all()
  {
    return ALL;
  }
  



  public static <C extends Comparable> ImmutableRangeSet<C> of(Range<C> range)
  {
    Preconditions.checkNotNull(range);
    if (range.isEmpty())
      return of();
    if (range.equals(Range.all())) {
      return all();
    }
    return new ImmutableRangeSet(ImmutableList.of(range));
  }
  



  public static <C extends Comparable> ImmutableRangeSet<C> copyOf(RangeSet<C> rangeSet)
  {
    Preconditions.checkNotNull(rangeSet);
    if (rangeSet.isEmpty())
      return of();
    if (rangeSet.encloses(Range.all())) {
      return all();
    }
    
    if ((rangeSet instanceof ImmutableRangeSet)) {
      ImmutableRangeSet<C> immutableRangeSet = (ImmutableRangeSet)rangeSet;
      if (!immutableRangeSet.isPartialView()) {
        return immutableRangeSet;
      }
    }
    return new ImmutableRangeSet(ImmutableList.copyOf(rangeSet.asRanges()));
  }
  
  ImmutableRangeSet(ImmutableList<Range<C>> ranges) {
    this.ranges = ranges;
  }
  
  private ImmutableRangeSet(ImmutableList<Range<C>> ranges, ImmutableRangeSet<C> complement) {
    this.ranges = ranges;
    this.complement = complement;
  }
  


  public boolean encloses(Range<C> otherRange)
  {
    int index = SortedLists.binarySearch(ranges, Range.lowerBoundFn(), lowerBound, Ordering.natural(), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
    




    return (index != -1) && (((Range)ranges.get(index)).encloses(otherRange));
  }
  
  public Range<C> rangeContaining(C value)
  {
    int index = SortedLists.binarySearch(ranges, Range.lowerBoundFn(), Cut.belowValue(value), Ordering.natural(), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
    




    if (index != -1) {
      Range<C> range = (Range)ranges.get(index);
      return range.contains(value) ? range : null;
    }
    return null;
  }
  
  public Range<C> span()
  {
    if (ranges.isEmpty()) {
      throw new NoSuchElementException();
    }
    return Range.create(ranges.get(0)).lowerBound, ranges.get(ranges.size() - 1)).upperBound);
  }
  


  public boolean isEmpty()
  {
    return ranges.isEmpty();
  }
  
  public void add(Range<C> range)
  {
    throw new UnsupportedOperationException();
  }
  
  public void addAll(RangeSet<C> other)
  {
    throw new UnsupportedOperationException();
  }
  
  public void remove(Range<C> range)
  {
    throw new UnsupportedOperationException();
  }
  
  public void removeAll(RangeSet<C> other)
  {
    throw new UnsupportedOperationException();
  }
  
  public ImmutableSet<Range<C>> asRanges()
  {
    if (ranges.isEmpty()) {
      return ImmutableSet.of();
    }
    return new RegularImmutableSortedSet(ranges, Range.RANGE_LEX_ORDERING);
  }
  

  private final class ComplementRanges
    extends ImmutableList<Range<C>>
  {
    private final boolean positiveBoundedBelow;
    
    private final boolean positiveBoundedAbove;
    
    private final int size;
    
    ComplementRanges()
    {
      positiveBoundedBelow = ((Range)ranges.get(0)).hasLowerBound();
      positiveBoundedAbove = ((Range)Iterables.getLast(ranges)).hasUpperBound();
      
      int size = ranges.size() - 1;
      if (positiveBoundedBelow) {
        size++;
      }
      if (positiveBoundedAbove) {
        size++;
      }
      this.size = size;
    }
    
    public int size()
    {
      return size;
    }
    
    public Range<C> get(int index)
    {
      Preconditions.checkElementIndex(index, size);
      Cut<C> lowerBound;
      Cut<C> lowerBound;
      if (positiveBoundedBelow) {
        lowerBound = index == 0 ? Cut.belowAll() : ranges.get(index - 1)).upperBound;
      } else {
        lowerBound = ranges.get(index)).upperBound;
      }
      Cut<C> upperBound;
      Cut<C> upperBound;
      if ((positiveBoundedAbove) && (index == size - 1)) {
        upperBound = Cut.aboveAll();
      } else {
        upperBound = ranges.get(index + (positiveBoundedBelow ? 0 : 1))).lowerBound;
      }
      
      return Range.create(lowerBound, upperBound);
    }
    
    boolean isPartialView()
    {
      return true;
    }
  }
  
  public ImmutableRangeSet<C> complement()
  {
    ImmutableRangeSet<C> result = complement;
    if (result != null)
      return result;
    if (ranges.isEmpty())
      return this.complement = all();
    if ((ranges.size() == 1) && (((Range)ranges.get(0)).equals(Range.all()))) {
      return this.complement = of();
    }
    ImmutableList<Range<C>> complementRanges = new ComplementRanges();
    result = this.complement = new ImmutableRangeSet(complementRanges, this);
    
    return result;
  }
  



  private ImmutableList<Range<C>> intersectRanges(final Range<C> range)
  {
    if ((ranges.isEmpty()) || (range.isEmpty()))
      return ImmutableList.of();
    if (range.encloses(span())) {
      return ranges;
    }
    int fromIndex;
    final int fromIndex;
    if (range.hasLowerBound()) {
      fromIndex = SortedLists.binarySearch(ranges, Range.upperBoundFn(), lowerBound, SortedLists.KeyPresentBehavior.FIRST_AFTER, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
    }
    else
    {
      fromIndex = 0;
    }
    int toIndex;
    int toIndex;
    if (range.hasUpperBound()) {
      toIndex = SortedLists.binarySearch(ranges, Range.lowerBoundFn(), upperBound, SortedLists.KeyPresentBehavior.FIRST_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
    }
    else
    {
      toIndex = ranges.size();
    }
    final int length = toIndex - fromIndex;
    if (length == 0) {
      return ImmutableList.of();
    }
    new ImmutableList()
    {
      public int size() {
        return length;
      }
      
      public Range<C> get(int index)
      {
        Preconditions.checkElementIndex(index, length);
        if ((index == 0) || (index == length - 1)) {
          return ((Range)ranges.get(index + fromIndex)).intersection(range);
        }
        return (Range)ranges.get(index + fromIndex);
      }
      

      boolean isPartialView()
      {
        return true;
      }
    };
  }
  




  public ImmutableRangeSet<C> subRangeSet(Range<C> range)
  {
    if (!isEmpty()) {
      Range<C> span = span();
      if (range.encloses(span))
        return this;
      if (range.isConnected(span)) {
        return new ImmutableRangeSet(intersectRanges(range));
      }
    }
    return of();
  }
  


















  public ImmutableSortedSet<C> asSet(DiscreteDomain<C> domain)
  {
    Preconditions.checkNotNull(domain);
    if (isEmpty()) {
      return ImmutableSortedSet.of();
    }
    Range<C> span = span().canonical(domain);
    if (!span.hasLowerBound())
    {

      throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded below");
    }
    if (!span.hasUpperBound()) {
      try {
        domain.maxValue();
      } catch (NoSuchElementException e) {
        throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded above");
      }
    }
    

    return new AsSet(domain);
  }
  
  private final class AsSet extends ImmutableSortedSet<C> {
    private final DiscreteDomain<C> domain;
    private transient Integer size;
    
    AsSet() { super();
      this.domain = domain;
    }
    



    public int size()
    {
      Integer result = size;
      if (result == null) {
        long total = 0L;
        for (Range<C> range : ranges) {
          total += ContiguousSet.create(range, domain).size();
          if (total >= 2147483647L) {
            break;
          }
        }
        result = this.size = Integer.valueOf(Ints.saturatedCast(total));
      }
      return result.intValue();
    }
    
    public UnmodifiableIterator<C> iterator()
    {
      new AbstractIterator() {
        final Iterator<Range<C>> rangeItr = ranges.iterator();
        Iterator<C> elemItr = Iterators.emptyIterator();
        
        protected C computeNext()
        {
          while (!elemItr.hasNext()) {
            if (rangeItr.hasNext()) {
              elemItr = ContiguousSet.create((Range)rangeItr.next(), domain).iterator();
            } else {
              return (Comparable)endOfData();
            }
          }
          return (Comparable)elemItr.next();
        }
      };
    }
    
    @GwtIncompatible("NavigableSet")
    public UnmodifiableIterator<C> descendingIterator()
    {
      new AbstractIterator() {
        final Iterator<Range<C>> rangeItr = ranges.reverse().iterator();
        Iterator<C> elemItr = Iterators.emptyIterator();
        
        protected C computeNext()
        {
          while (!elemItr.hasNext()) {
            if (rangeItr.hasNext()) {
              elemItr = ContiguousSet.create((Range)rangeItr.next(), domain).descendingIterator();
            } else {
              return (Comparable)endOfData();
            }
          }
          return (Comparable)elemItr.next();
        }
      };
    }
    
    ImmutableSortedSet<C> subSet(Range<C> range) {
      return subRangeSet(range).asSet(domain);
    }
    
    ImmutableSortedSet<C> headSetImpl(C toElement, boolean inclusive)
    {
      return subSet(Range.upTo(toElement, BoundType.forBoolean(inclusive)));
    }
    

    ImmutableSortedSet<C> subSetImpl(C fromElement, boolean fromInclusive, C toElement, boolean toInclusive)
    {
      if ((!fromInclusive) && (!toInclusive) && (Range.compareOrThrow(fromElement, toElement) == 0)) {
        return ImmutableSortedSet.of();
      }
      return subSet(Range.range(fromElement, BoundType.forBoolean(fromInclusive), toElement, BoundType.forBoolean(toInclusive)));
    }
    


    ImmutableSortedSet<C> tailSetImpl(C fromElement, boolean inclusive)
    {
      return subSet(Range.downTo(fromElement, BoundType.forBoolean(inclusive)));
    }
    
    public boolean contains(@Nullable Object o)
    {
      if (o == null) {
        return false;
      }
      try
      {
        C c = (Comparable)o;
        return contains(c);
      } catch (ClassCastException e) {}
      return false;
    }
    

    int indexOf(Object target)
    {
      if (contains(target))
      {
        C c = (Comparable)target;
        long total = 0L;
        for (Range<C> range : ranges) {
          if (range.contains(c)) {
            return Ints.saturatedCast(total + ContiguousSet.create(range, domain).indexOf(c));
          }
          total += ContiguousSet.create(range, domain).size();
        }
        
        throw new AssertionError("impossible");
      }
      return -1;
    }
    
    boolean isPartialView()
    {
      return ranges.isPartialView();
    }
    
    public String toString()
    {
      return ranges.toString();
    }
    
    Object writeReplace()
    {
      return new ImmutableRangeSet.AsSetSerializedForm(ranges, domain);
    }
  }
  
  private static class AsSetSerializedForm<C extends Comparable> implements Serializable {
    private final ImmutableList<Range<C>> ranges;
    private final DiscreteDomain<C> domain;
    
    AsSetSerializedForm(ImmutableList<Range<C>> ranges, DiscreteDomain<C> domain) {
      this.ranges = ranges;
      this.domain = domain;
    }
    
    Object readResolve() {
      return new ImmutableRangeSet(ranges).asSet(domain);
    }
  }
  





  boolean isPartialView()
  {
    return ranges.isPartialView();
  }
  


  public static <C extends Comparable<?>> Builder<C> builder()
  {
    return new Builder();
  }
  

  public static class Builder<C extends Comparable<?>>
  {
    private final RangeSet<C> rangeSet;
    
    public Builder()
    {
      rangeSet = TreeRangeSet.create();
    }
    






    public Builder<C> add(Range<C> range)
    {
      if (range.isEmpty())
        throw new IllegalArgumentException("range must not be empty, but was " + range);
      if (!rangeSet.complement().encloses(range)) {
        for (Range<C> currentRange : rangeSet.asRanges()) {
          Preconditions.checkArgument((!currentRange.isConnected(range)) || (currentRange.intersection(range).isEmpty()), "Ranges may not overlap, but received %s and %s", new Object[] { currentRange, range });
        }
        

        throw new AssertionError("should have thrown an IAE above");
      }
      rangeSet.add(range);
      return this;
    }
    



    public Builder<C> addAll(RangeSet<C> ranges)
    {
      for (Range<C> range : ranges.asRanges()) {
        add(range);
      }
      return this;
    }
    


    public ImmutableRangeSet<C> build()
    {
      return ImmutableRangeSet.copyOf(rangeSet);
    }
  }
  
  private static final class SerializedForm<C extends Comparable> implements Serializable {
    private final ImmutableList<Range<C>> ranges;
    
    SerializedForm(ImmutableList<Range<C>> ranges) {
      this.ranges = ranges;
    }
    
    Object readResolve() {
      if (ranges.isEmpty())
        return ImmutableRangeSet.of();
      if (ranges.equals(ImmutableList.of(Range.all()))) {
        return ImmutableRangeSet.all();
      }
      return new ImmutableRangeSet(ranges);
    }
  }
  
  Object writeReplace()
  {
    return new SerializedForm(ranges);
  }
}
