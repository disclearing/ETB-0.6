package com.google.common.util.concurrent;

import java.util.concurrent.Callable;


























public abstract class ForwardingListeningExecutorService
  extends ForwardingExecutorService
  implements ListeningExecutorService
{
  protected ForwardingListeningExecutorService() {}
  
  protected abstract ListeningExecutorService delegate();
  
  public <T> ListenableFuture<T> submit(Callable<T> task)
  {
    return delegate().submit(task);
  }
  
  public ListenableFuture<?> submit(Runnable task)
  {
    return delegate().submit(task);
  }
  
  public <T> ListenableFuture<T> submit(Runnable task, T result)
  {
    return delegate().submit(task, result);
  }
}
