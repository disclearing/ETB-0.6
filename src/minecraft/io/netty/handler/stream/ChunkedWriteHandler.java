package io.netty.handler.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;

















































public class ChunkedWriteHandler
  extends ChannelDuplexHandler
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChunkedWriteHandler.class);
  

  private final Queue<PendingWrite> queue = new ArrayDeque();
  
  private volatile ChannelHandlerContext ctx;
  
  private PendingWrite currentWrite;
  

  public ChunkedWriteHandler() {}
  
  @Deprecated
  public ChunkedWriteHandler(int maxPendingWrites)
  {
    if (maxPendingWrites <= 0) {
      throw new IllegalArgumentException("maxPendingWrites: " + maxPendingWrites + " (expected: > 0)");
    }
  }
  
  public void handlerAdded(ChannelHandlerContext ctx)
    throws Exception
  {
    this.ctx = ctx;
  }
  


  public void resumeTransfer()
  {
    final ChannelHandlerContext ctx = this.ctx;
    if (ctx == null) {
      return;
    }
    if (ctx.executor().inEventLoop()) {
      try {
        doFlush(ctx);
      } catch (Exception e) {
        if (logger.isWarnEnabled()) {
          logger.warn("Unexpected exception while sending chunks.", e);
        }
        
      }
    } else {
      ctx.executor().execute(new Runnable()
      {
        public void run()
        {
          try {
            ChunkedWriteHandler.this.doFlush(ctx);
          } catch (Exception e) {
            if (ChunkedWriteHandler.logger.isWarnEnabled()) {
              ChunkedWriteHandler.logger.warn("Unexpected exception while sending chunks.", e);
            }
          }
        }
      });
    }
  }
  
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    queue.add(new PendingWrite(msg, promise));
  }
  
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    Channel channel = ctx.channel();
    if ((channel.isWritable()) || (!channel.isActive())) {
      doFlush(ctx);
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    doFlush(ctx);
    super.channelInactive(ctx);
  }
  
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
  {
    if (ctx.channel().isWritable())
    {
      doFlush(ctx);
    }
    ctx.fireChannelWritabilityChanged();
  }
  
  private void discard(Throwable cause) {
    for (;;) {
      PendingWrite currentWrite = this.currentWrite;
      
      if (this.currentWrite == null) {
        currentWrite = (PendingWrite)queue.poll();
      } else {
        this.currentWrite = null;
      }
      
      if (currentWrite == null) {
        break;
      }
      Object message = msg;
      if ((message instanceof ChunkedInput)) {
        ChunkedInput<?> in = (ChunkedInput)message;
        try {
          if (!in.isEndOfInput()) {
            if (cause == null) {
              cause = new ClosedChannelException();
            }
            currentWrite.fail(cause);
          } else {
            currentWrite.success();
          }
          closeInput(in);
        } catch (Exception e) {
          currentWrite.fail(e);
          logger.warn(ChunkedInput.class.getSimpleName() + ".isEndOfInput() failed", e);
          closeInput(in);
        }
      } else {
        if (cause == null) {
          cause = new ClosedChannelException();
        }
        currentWrite.fail(cause);
      }
    }
  }
  
  private void doFlush(ChannelHandlerContext ctx) throws Exception {
    final Channel channel = ctx.channel();
    if (!channel.isActive()) {
      discard(null);
      return;
    }
    while (channel.isWritable()) {
      if (this.currentWrite == null) {
        this.currentWrite = ((PendingWrite)queue.poll());
      }
      
      if (this.currentWrite == null) {
        break;
      }
      final PendingWrite currentWrite = this.currentWrite;
      final Object pendingMessage = msg;
      
      if ((pendingMessage instanceof ChunkedInput)) {
        final ChunkedInput<?> chunks = (ChunkedInput)pendingMessage;
        

        Object message = null;
        boolean endOfInput;
        boolean suspend; try { message = chunks.readChunk(ctx);
          endOfInput = chunks.isEndOfInput();
          boolean suspend;
          if (message == null)
          {
            suspend = !endOfInput;
          } else {
            suspend = false;
          }
        } catch (Throwable t) {
          this.currentWrite = null;
          
          if (message != null) {
            ReferenceCountUtil.release(message);
          }
          
          currentWrite.fail(t);
          closeInput(chunks);
          break;
        }
        
        if (suspend) {
          break;
        }
        



        if (message == null)
        {

          message = Unpooled.EMPTY_BUFFER;
        }
        
        final int amount = amount(message);
        ChannelFuture f = ctx.write(message);
        if (endOfInput) {
          this.currentWrite = null;
          





          f.addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              currentWrite.progress(amount);
              currentWrite.success();
              ChunkedWriteHandler.closeInput(chunks);
            }
          });
        } else if (channel.isWritable()) {
          f.addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              if (!future.isSuccess()) {
                ChunkedWriteHandler.closeInput((ChunkedInput)pendingMessage);
                currentWrite.fail(future.cause());
              } else {
                currentWrite.progress(amount);
              }
            }
          });
        } else {
          f.addListener(new ChannelFutureListener()
          {
            public void operationComplete(ChannelFuture future) throws Exception {
              if (!future.isSuccess()) {
                ChunkedWriteHandler.closeInput((ChunkedInput)pendingMessage);
                currentWrite.fail(future.cause());
              } else {
                currentWrite.progress(amount);
                if (channel.isWritable()) {
                  resumeTransfer();
                }
              }
            }
          });
        }
      } else {
        ctx.write(pendingMessage, promise);
        this.currentWrite = null;
      }
      

      ctx.flush();
      
      if (!channel.isActive()) {
        discard(new ClosedChannelException());
        return;
      }
    }
  }
  
  static void closeInput(ChunkedInput<?> chunks) {
    try {
      chunks.close();
    } catch (Throwable t) {
      if (logger.isWarnEnabled()) {
        logger.warn("Failed to close a chunked input.", t);
      }
    }
  }
  
  private static final class PendingWrite {
    final Object msg;
    final ChannelPromise promise;
    private long progress;
    
    PendingWrite(Object msg, ChannelPromise promise) {
      this.msg = msg;
      this.promise = promise;
    }
    
    void fail(Throwable cause) {
      ReferenceCountUtil.release(msg);
      promise.tryFailure(cause);
    }
    
    void success() {
      if (promise.isDone())
      {
        return;
      }
      
      if ((promise instanceof ChannelProgressivePromise))
      {
        ((ChannelProgressivePromise)promise).tryProgress(progress, progress);
      }
      
      promise.trySuccess();
    }
    
    void progress(int amount) {
      progress += amount;
      if ((promise instanceof ChannelProgressivePromise)) {
        ((ChannelProgressivePromise)promise).tryProgress(progress, -1L);
      }
    }
  }
  
  private static int amount(Object msg) {
    if ((msg instanceof ByteBuf)) {
      return ((ByteBuf)msg).readableBytes();
    }
    if ((msg instanceof ByteBufHolder)) {
      return ((ByteBufHolder)msg).content().readableBytes();
    }
    return 1;
  }
}
