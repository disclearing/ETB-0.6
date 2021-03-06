package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import java.util.List;





































public class HttpObjectAggregator
  extends MessageToMessageDecoder<HttpObject>
{
  public static final int DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS = 1024;
  private static final FullHttpResponse CONTINUE = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);
  
  private final int maxContentLength;
  
  private AggregatedFullHttpMessage currentMessage;
  
  private boolean tooLongFrameFound;
  private int maxCumulationBufferComponents = 1024;
  



  private ChannelHandlerContext ctx;
  



  public HttpObjectAggregator(int maxContentLength)
  {
    if (maxContentLength <= 0) {
      throw new IllegalArgumentException("maxContentLength must be a positive integer: " + maxContentLength);
    }
    

    this.maxContentLength = maxContentLength;
  }
  





  public final int getMaxCumulationBufferComponents()
  {
    return maxCumulationBufferComponents;
  }
  






  public final void setMaxCumulationBufferComponents(int maxCumulationBufferComponents)
  {
    if (maxCumulationBufferComponents < 2) {
      throw new IllegalArgumentException("maxCumulationBufferComponents: " + maxCumulationBufferComponents + " (expected: >= 2)");
    }
    


    if (ctx == null) {
      this.maxCumulationBufferComponents = maxCumulationBufferComponents;
    } else {
      throw new IllegalStateException("decoder properties cannot be changed once the decoder is added to a pipeline.");
    }
  }
  
  protected void decode(final ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    AggregatedFullHttpMessage currentMessage = this.currentMessage;
    
    if ((msg instanceof HttpMessage)) {
      tooLongFrameFound = false;
      assert (currentMessage == null);
      
      HttpMessage m = (HttpMessage)msg;
      





      if (HttpHeaders.is100ContinueExpected(m)) {
        ctx.writeAndFlush(CONTINUE).addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              ctx.fireExceptionCaught(future.cause());
            }
          }
        });
      }
      
      if (!m.getDecoderResult().isSuccess()) {
        HttpHeaders.removeTransferEncodingChunked(m);
        out.add(toFullMessage(m));
        this.currentMessage = null;
        return;
      }
      if ((msg instanceof HttpRequest)) {
        HttpRequest header = (HttpRequest)msg;
        this.currentMessage = (currentMessage = new AggregatedFullHttpRequest(header, ctx.alloc().compositeBuffer(maxCumulationBufferComponents), null, null));
      }
      else if ((msg instanceof HttpResponse)) {
        HttpResponse header = (HttpResponse)msg;
        this.currentMessage = (currentMessage = new AggregatedFullHttpResponse(header, Unpooled.compositeBuffer(maxCumulationBufferComponents), null, null));
      }
      else
      {
        throw new Error();
      }
      

      HttpHeaders.removeTransferEncodingChunked(currentMessage);
    } else if ((msg instanceof HttpContent)) {
      if (tooLongFrameFound) {
        if ((msg instanceof LastHttpContent)) {
          this.currentMessage = null;
        }
        
        return;
      }
      assert (currentMessage != null);
      

      HttpContent chunk = (HttpContent)msg;
      CompositeByteBuf content = (CompositeByteBuf)currentMessage.content();
      
      if (content.readableBytes() > maxContentLength - chunk.content().readableBytes()) {
        tooLongFrameFound = true;
        

        currentMessage.release();
        this.currentMessage = null;
        
        throw new TooLongFrameException("HTTP content length exceeded " + maxContentLength + " bytes.");
      }
      



      if (chunk.content().isReadable()) {
        chunk.retain();
        content.addComponent(chunk.content());
        content.writerIndex(content.writerIndex() + chunk.content().readableBytes());
      }
      boolean last;
      boolean last;
      if (!chunk.getDecoderResult().isSuccess()) {
        currentMessage.setDecoderResult(DecoderResult.failure(chunk.getDecoderResult().cause()));
        
        last = true;
      } else {
        last = chunk instanceof LastHttpContent;
      }
      
      if (last) {
        this.currentMessage = null;
        

        if ((chunk instanceof LastHttpContent)) {
          LastHttpContent trailer = (LastHttpContent)chunk;
          currentMessage.setTrailingHeaders(trailer.trailingHeaders());
        } else {
          currentMessage.setTrailingHeaders(new DefaultHttpHeaders());
        }
        

        currentMessage.headers().set("Content-Length", String.valueOf(content.readableBytes()));
        



        out.add(currentMessage);
      }
    } else {
      throw new Error();
    }
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    super.channelInactive(ctx);
    

    if (currentMessage != null) {
      currentMessage.release();
      currentMessage = null;
    }
  }
  
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    this.ctx = ctx;
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    super.handlerRemoved(ctx);
    

    if (currentMessage != null) {
      currentMessage.release();
      currentMessage = null;
    }
  }
  
  private static FullHttpMessage toFullMessage(HttpMessage msg) {
    if ((msg instanceof FullHttpMessage)) {
      return ((FullHttpMessage)msg).retain();
    }
    
    FullHttpMessage fullMsg;
    if ((msg instanceof HttpRequest)) {
      fullMsg = new AggregatedFullHttpRequest((HttpRequest)msg, Unpooled.EMPTY_BUFFER, new DefaultHttpHeaders(), null);
    } else { FullHttpMessage fullMsg;
      if ((msg instanceof HttpResponse)) {
        fullMsg = new AggregatedFullHttpResponse((HttpResponse)msg, Unpooled.EMPTY_BUFFER, new DefaultHttpHeaders(), null);
      }
      else
        throw new IllegalStateException();
    }
    FullHttpMessage fullMsg;
    return fullMsg;
  }
  
  private static abstract class AggregatedFullHttpMessage extends DefaultByteBufHolder implements FullHttpMessage {
    protected final HttpMessage message;
    private HttpHeaders trailingHeaders;
    
    private AggregatedFullHttpMessage(HttpMessage message, ByteBuf content, HttpHeaders trailingHeaders) {
      super();
      this.message = message;
      this.trailingHeaders = trailingHeaders;
    }
    
    public HttpHeaders trailingHeaders() {
      return trailingHeaders;
    }
    
    public void setTrailingHeaders(HttpHeaders trailingHeaders) {
      this.trailingHeaders = trailingHeaders;
    }
    
    public HttpVersion getProtocolVersion()
    {
      return message.getProtocolVersion();
    }
    
    public FullHttpMessage setProtocolVersion(HttpVersion version)
    {
      message.setProtocolVersion(version);
      return this;
    }
    
    public HttpHeaders headers()
    {
      return message.headers();
    }
    
    public DecoderResult getDecoderResult()
    {
      return message.getDecoderResult();
    }
    
    public void setDecoderResult(DecoderResult result)
    {
      message.setDecoderResult(result);
    }
    
    public FullHttpMessage retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpMessage retain()
    {
      super.retain();
      return this;
    }
    
    public abstract FullHttpMessage copy();
    
    public abstract FullHttpMessage duplicate();
  }
  
  private static final class AggregatedFullHttpRequest
    extends HttpObjectAggregator.AggregatedFullHttpMessage implements FullHttpRequest
  {
    private AggregatedFullHttpRequest(HttpRequest request, ByteBuf content, HttpHeaders trailingHeaders)
    {
      super(content, trailingHeaders, null);
    }
    
    public FullHttpRequest copy()
    {
      DefaultFullHttpRequest copy = new DefaultFullHttpRequest(getProtocolVersion(), getMethod(), getUri(), content().copy());
      
      copy.headers().set(headers());
      copy.trailingHeaders().set(trailingHeaders());
      return copy;
    }
    
    public FullHttpRequest duplicate()
    {
      DefaultFullHttpRequest duplicate = new DefaultFullHttpRequest(getProtocolVersion(), getMethod(), getUri(), content().duplicate());
      
      duplicate.headers().set(headers());
      duplicate.trailingHeaders().set(trailingHeaders());
      return duplicate;
    }
    
    public FullHttpRequest retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpRequest retain()
    {
      super.retain();
      return this;
    }
    
    public FullHttpRequest setMethod(HttpMethod method)
    {
      ((HttpRequest)message).setMethod(method);
      return this;
    }
    
    public FullHttpRequest setUri(String uri)
    {
      ((HttpRequest)message).setUri(uri);
      return this;
    }
    
    public HttpMethod getMethod()
    {
      return ((HttpRequest)message).getMethod();
    }
    
    public String getUri()
    {
      return ((HttpRequest)message).getUri();
    }
    
    public FullHttpRequest setProtocolVersion(HttpVersion version)
    {
      super.setProtocolVersion(version);
      return this;
    }
  }
  
  private static final class AggregatedFullHttpResponse extends HttpObjectAggregator.AggregatedFullHttpMessage implements FullHttpResponse
  {
    private AggregatedFullHttpResponse(HttpResponse message, ByteBuf content, HttpHeaders trailingHeaders) {
      super(content, trailingHeaders, null);
    }
    
    public FullHttpResponse copy()
    {
      DefaultFullHttpResponse copy = new DefaultFullHttpResponse(getProtocolVersion(), getStatus(), content().copy());
      
      copy.headers().set(headers());
      copy.trailingHeaders().set(trailingHeaders());
      return copy;
    }
    
    public FullHttpResponse duplicate()
    {
      DefaultFullHttpResponse duplicate = new DefaultFullHttpResponse(getProtocolVersion(), getStatus(), content().duplicate());
      
      duplicate.headers().set(headers());
      duplicate.trailingHeaders().set(trailingHeaders());
      return duplicate;
    }
    
    public FullHttpResponse setStatus(HttpResponseStatus status)
    {
      ((HttpResponse)message).setStatus(status);
      return this;
    }
    
    public HttpResponseStatus getStatus()
    {
      return ((HttpResponse)message).getStatus();
    }
    
    public FullHttpResponse setProtocolVersion(HttpVersion version)
    {
      super.setProtocolVersion(version);
      return this;
    }
    
    public FullHttpResponse retain(int increment)
    {
      super.retain(increment);
      return this;
    }
    
    public FullHttpResponse retain()
    {
      super.retain();
      return this;
    }
  }
}
