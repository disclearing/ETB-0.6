package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.ReferenceCountUtil;
import java.util.List;
































public abstract class HttpContentDecoder
  extends MessageToMessageDecoder<HttpObject>
{
  private EmbeddedChannel decoder;
  private HttpMessage message;
  private boolean decodeStarted;
  private boolean continueResponse;
  
  public HttpContentDecoder() {}
  
  protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception
  {
    if (((msg instanceof HttpResponse)) && (((HttpResponse)msg).getStatus().code() == 100))
    {
      if (!(msg instanceof LastHttpContent)) {
        continueResponse = true;
      }
      
      out.add(ReferenceCountUtil.retain(msg));
      return;
    }
    
    if (continueResponse) {
      if ((msg instanceof LastHttpContent)) {
        continueResponse = false;
      }
      
      out.add(ReferenceCountUtil.retain(msg));
      return;
    }
    
    if ((msg instanceof HttpMessage)) {
      assert (this.message == null);
      this.message = ((HttpMessage)msg);
      decodeStarted = false;
      cleanup();
    }
    
    if ((msg instanceof HttpContent)) {
      HttpContent c = (HttpContent)msg;
      
      if (!decodeStarted) {
        decodeStarted = true;
        HttpMessage message = this.message;
        HttpHeaders headers = message.headers();
        this.message = null;
        

        String contentEncoding = headers.get("Content-Encoding");
        if (contentEncoding != null) {
          contentEncoding = contentEncoding.trim();
        } else {
          contentEncoding = "identity";
        }
        
        if ((this.decoder = newContentDecoder(contentEncoding)) != null)
        {

          String targetContentEncoding = getTargetContentEncoding(contentEncoding);
          if ("identity".equals(targetContentEncoding))
          {

            headers.remove("Content-Encoding");
          } else {
            headers.set("Content-Encoding", targetContentEncoding);
          }
          
          out.add(message);
          decodeContent(c, out);
          

          if (headers.contains("Content-Length")) {
            int contentLength = 0;
            int size = out.size();
            for (int i = 0; i < size; i++) {
              Object o = out.get(i);
              if ((o instanceof HttpContent)) {
                contentLength += ((HttpContent)o).content().readableBytes();
              }
            }
            headers.set("Content-Length", Integer.toString(contentLength));
          }
          

          return;
        }
        
        if ((c instanceof LastHttpContent)) {
          decodeStarted = false;
        }
        out.add(message);
        out.add(c.retain());
        return;
      }
      
      if (decoder != null) {
        decodeContent(c, out);
      } else {
        if ((c instanceof LastHttpContent)) {
          decodeStarted = false;
        }
        out.add(c.retain());
      }
    }
  }
  
  private void decodeContent(HttpContent c, List<Object> out) {
    ByteBuf content = c.content();
    
    decode(content, out);
    
    if ((c instanceof LastHttpContent)) {
      finishDecode(out);
      
      LastHttpContent last = (LastHttpContent)c;
      

      HttpHeaders headers = last.trailingHeaders();
      if (headers.isEmpty()) {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      } else {
        out.add(new ComposedLastHttpContent(headers));
      }
    }
  }
  








  protected abstract EmbeddedChannel newContentDecoder(String paramString)
    throws Exception;
  







  protected String getTargetContentEncoding(String contentEncoding)
    throws Exception
  {
    return "identity";
  }
  
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception
  {
    cleanup();
    super.handlerRemoved(ctx);
  }
  
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    cleanup();
    super.channelInactive(ctx);
  }
  
  private void cleanup() {
    if (decoder != null)
    {
      if (decoder.finish()) {
        for (;;) {
          ByteBuf buf = (ByteBuf)decoder.readOutbound();
          if (buf == null) {
            break;
          }
          
          buf.release();
        }
      }
      decoder = null;
    }
  }
  
  private void decode(ByteBuf in, List<Object> out)
  {
    decoder.writeInbound(new Object[] { in.retain() });
    fetchDecoderOutput(out);
  }
  
  private void finishDecode(List<Object> out) {
    if (decoder.finish()) {
      fetchDecoderOutput(out);
    }
    decodeStarted = false;
    decoder = null;
  }
  
  private void fetchDecoderOutput(List<Object> out) {
    for (;;) {
      ByteBuf buf = (ByteBuf)decoder.readInbound();
      if (buf == null) {
        break;
      }
      if (!buf.isReadable()) {
        buf.release();
      }
      else {
        out.add(new DefaultHttpContent(buf));
      }
    }
  }
}
