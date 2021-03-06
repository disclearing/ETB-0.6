package io.netty.handler.codec.spdy;

import io.netty.util.internal.StringUtil;
import java.util.Map.Entry;



















public class DefaultSpdyHeadersFrame
  extends DefaultSpdyStreamFrame
  implements SpdyHeadersFrame
{
  private boolean invalid;
  private boolean truncated;
  private final SpdyHeaders headers = new DefaultSpdyHeaders();
  




  public DefaultSpdyHeadersFrame(int streamId)
  {
    super(streamId);
  }
  
  public SpdyHeadersFrame setStreamId(int streamId)
  {
    super.setStreamId(streamId);
    return this;
  }
  
  public SpdyHeadersFrame setLast(boolean last)
  {
    super.setLast(last);
    return this;
  }
  
  public boolean isInvalid()
  {
    return invalid;
  }
  
  public SpdyHeadersFrame setInvalid()
  {
    invalid = true;
    return this;
  }
  
  public boolean isTruncated()
  {
    return truncated;
  }
  
  public SpdyHeadersFrame setTruncated()
  {
    truncated = true;
    return this;
  }
  
  public SpdyHeaders headers()
  {
    return headers;
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder();
    buf.append(StringUtil.simpleClassName(this));
    buf.append("(last: ");
    buf.append(isLast());
    buf.append(')');
    buf.append(StringUtil.NEWLINE);
    buf.append("--> Stream-ID = ");
    buf.append(streamId());
    buf.append(StringUtil.NEWLINE);
    buf.append("--> Headers:");
    buf.append(StringUtil.NEWLINE);
    appendHeaders(buf);
    

    buf.setLength(buf.length() - StringUtil.NEWLINE.length());
    return buf.toString();
  }
  
  protected void appendHeaders(StringBuilder buf) {
    for (Map.Entry<String, String> e : headers()) {
      buf.append("    ");
      buf.append((String)e.getKey());
      buf.append(": ");
      buf.append((String)e.getValue());
      buf.append(StringUtil.NEWLINE);
    }
  }
}
