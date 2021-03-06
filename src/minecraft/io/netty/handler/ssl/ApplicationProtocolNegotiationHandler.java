package io.netty.handler.ssl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;




















































public abstract class ApplicationProtocolNegotiationHandler
  extends ChannelInboundHandlerAdapter
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ApplicationProtocolNegotiationHandler.class);
  



  private final String fallbackProtocol;
  



  protected ApplicationProtocolNegotiationHandler(String fallbackProtocol)
  {
    this.fallbackProtocol = ((String)ObjectUtil.checkNotNull(fallbackProtocol, "fallbackProtocol"));
  }
  
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
  {
    if ((evt instanceof SslHandshakeCompletionEvent)) {
      ctx.pipeline().remove(this);
      
      SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent)evt;
      if (handshakeEvent.isSuccess()) {
        SslHandler sslHandler = (SslHandler)ctx.pipeline().get(SslHandler.class);
        if (sslHandler == null) {
          throw new IllegalStateException("cannot find a SslHandler in the pipeline (required for application-level protocol negotiation)");
        }
        
        String protocol = sslHandler.applicationProtocol();
        configurePipeline(ctx, protocol != null ? protocol : fallbackProtocol);
      } else {
        handshakeFailure(ctx, handshakeEvent.cause());
      }
    }
    
    ctx.fireUserEventTriggered(evt);
  }
  




  protected abstract void configurePipeline(ChannelHandlerContext paramChannelHandlerContext, String paramString)
    throws Exception;
  




  protected void handshakeFailure(ChannelHandlerContext ctx, Throwable cause)
    throws Exception
  {
    logger.warn("{} TLS handshake failed:", ctx.channel(), cause);
    ctx.close();
  }
  
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    logger.warn("{} Failed to select the application-level protocol:", ctx.channel(), cause);
    ctx.close();
  }
}
