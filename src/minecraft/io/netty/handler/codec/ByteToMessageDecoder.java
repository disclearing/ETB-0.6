package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;
import java.util.List;
































public abstract class ByteToMessageDecoder
  extends ChannelInboundHandlerAdapter
{
  ByteBuf cumulation;
  private boolean singleDecode;
  private boolean decodeWasNull;
  private boolean first;
  
  protected ByteToMessageDecoder()
  {
    if (isSharable()) {
      throw new IllegalStateException("@Sharable annotation is not allowed");
    }
  }
  





  public void setSingleDecode(boolean singleDecode)
  {
    this.singleDecode = singleDecode;
  }
  





  public boolean isSingleDecode()
  {
    return singleDecode;
  }
  





  protected int actualReadableBytes()
  {
    return internalBuffer().readableBytes();
  }
  




  protected ByteBuf internalBuffer()
  {
    if (cumulation != null) {
      return cumulation;
    }
    return Unpooled.EMPTY_BUFFER;
  }
  
  public final void handlerRemoved(ChannelHandlerContext ctx)
    throws Exception
  {
    ByteBuf buf = internalBuffer();
    int readable = buf.readableBytes();
    if (buf.isReadable()) {
      ByteBuf bytes = buf.readBytes(readable);
      buf.release();
      ctx.fireChannelRead(bytes);
    } else {
      buf.release();
    }
    cumulation = null;
    ctx.fireChannelReadComplete();
    handlerRemoved0(ctx);
  }
  

  protected void handlerRemoved0(ChannelHandlerContext ctx)
    throws Exception
  {}
  
  public void channelRead(ChannelHandlerContext ctx, Object msg)
    throws Exception
  {
    if ((msg instanceof ByteBuf)) {
      RecyclableArrayList out = RecyclableArrayList.newInstance();
      try {
        ByteBuf data = (ByteBuf)msg;
        first = (cumulation == null);
        if (first) {
          cumulation = data;
        } else {
          if ((cumulation.writerIndex() > cumulation.maxCapacity() - data.readableBytes()) || (cumulation.refCnt() > 1))
          {







            expandCumulation(ctx, data.readableBytes());
          }
          cumulation.writeBytes(data);
          data.release();
        }
        callDecode(ctx, cumulation, out); } catch (DecoderException e) { int size;
        int i;
        throw e;
      } catch (Throwable t) {
        throw new DecoderException(t);
      } finally {
        if ((cumulation != null) && (!cumulation.isReadable())) {
          cumulation.release();
          cumulation = null;
        }
        int size = out.size();
        decodeWasNull = (size == 0);
        
        for (int i = 0; i < size; i++) {
          ctx.fireChannelRead(out.get(i));
        }
        out.recycle();
      }
    } else {
      ctx.fireChannelRead(msg);
    }
  }
  
  private void expandCumulation(ChannelHandlerContext ctx, int readable) {
    ByteBuf oldCumulation = cumulation;
    cumulation = ctx.alloc().buffer(oldCumulation.readableBytes() + readable);
    cumulation.writeBytes(oldCumulation);
    oldCumulation.release();
  }
  
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
  {
    if ((cumulation != null) && (!first) && (cumulation.refCnt() == 1))
    {






      cumulation.discardSomeReadBytes();
    }
    if (decodeWasNull) {
      decodeWasNull = false;
      if (!ctx.channel().config().isAutoRead()) {
        ctx.read();
      }
    }
    ctx.fireChannelReadComplete();
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    RecyclableArrayList out = RecyclableArrayList.newInstance();
    try {
      if (cumulation != null) {
        callDecode(ctx, cumulation, out);
        decodeLast(ctx, cumulation, out);
      } else {
        decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
      } } catch (DecoderException e) { int size;
      int i;
      throw e;
    } catch (Exception e) {
      throw new DecoderException(e);
    } finally {
      try {
        if (cumulation != null) {
          cumulation.release();
          cumulation = null;
        }
        int size = out.size();
        for (int i = 0; i < size; i++) {
          ctx.fireChannelRead(out.get(i));
        }
        if (size > 0)
        {
          ctx.fireChannelReadComplete();
        }
        ctx.fireChannelInactive();
      }
      finally {
        out.recycle();
      }
    }
  }
  






  protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
  {
    try
    {
      while (in.isReadable()) {
        int outSize = out.size();
        int oldInputLength = in.readableBytes();
        decode(ctx, in, out);
        




        if (ctx.isRemoved()) {
          break;
        }
        
        if (outSize == out.size()) {
          if (oldInputLength == in.readableBytes()) {
            break;
          }
          
        }
        else
        {
          if (oldInputLength == in.readableBytes()) {
            throw new DecoderException(StringUtil.simpleClassName(getClass()) + ".decode() did not read anything but decoded a message.");
          }
          


          if (isSingleDecode())
            break;
        }
      }
    } catch (DecoderException e) {
      throw e;
    } catch (Throwable cause) {
      throw new DecoderException(cause);
    }
  }
  







  protected abstract void decode(ChannelHandlerContext paramChannelHandlerContext, ByteBuf paramByteBuf, List<Object> paramList)
    throws Exception;
  







  protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    decode(ctx, in, out);
  }
}
