package io.netty.util.concurrent;

import java.util.LinkedHashSet;
import java.util.Set;































@Deprecated
public class PromiseAggregator<V, F extends Future<V>>
  implements GenericFutureListener<F>
{
  private final Promise<?> aggregatePromise;
  private final boolean failPending;
  private Set<Promise<V>> pendingPromises;
  
  public PromiseAggregator(Promise<Void> aggregatePromise, boolean failPending)
  {
    if (aggregatePromise == null) {
      throw new NullPointerException("aggregatePromise");
    }
    this.aggregatePromise = aggregatePromise;
    this.failPending = failPending;
  }
  



  public PromiseAggregator(Promise<Void> aggregatePromise)
  {
    this(aggregatePromise, true);
  }
  


  @SafeVarargs
  public final PromiseAggregator<V, F> add(Promise<V>... promises)
  {
    if (promises == null) {
      throw new NullPointerException("promises");
    }
    if (promises.length == 0) {
      return this;
    }
    synchronized (this) {
      if (pendingPromises == null) { int size;
        int size;
        if (promises.length > 1) {
          size = promises.length;
        } else {
          size = 2;
        }
        pendingPromises = new LinkedHashSet(size);
      }
      for (Promise<V> p : promises)
        if (p != null)
        {

          pendingPromises.add(p);
          p.addListener(this);
        }
    }
    return this;
  }
  
  public synchronized void operationComplete(F future) throws Exception
  {
    if (pendingPromises == null) {
      aggregatePromise.setSuccess(null);
    } else {
      pendingPromises.remove(future);
      Throwable cause; if (!future.isSuccess()) {
        cause = future.cause();
        aggregatePromise.setFailure(cause);
        if (failPending) {
          for (Promise<V> pendingFuture : pendingPromises) {
            pendingFuture.setFailure(cause);
          }
        }
      }
      else if (pendingPromises.isEmpty()) {
        aggregatePromise.setSuccess(null);
      }
    }
  }
}
