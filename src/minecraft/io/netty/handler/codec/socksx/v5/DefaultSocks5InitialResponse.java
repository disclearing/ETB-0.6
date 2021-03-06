package io.netty.handler.codec.socksx.v5;

import io.netty.handler.codec.DecoderResult;
import io.netty.util.internal.StringUtil;
















public class DefaultSocks5InitialResponse
  extends AbstractSocks5Message
  implements Socks5InitialResponse
{
  private final Socks5AuthMethod authMethod;
  
  public DefaultSocks5InitialResponse(Socks5AuthMethod authMethod)
  {
    if (authMethod == null) {
      throw new NullPointerException("authMethod");
    }
    this.authMethod = authMethod;
  }
  
  public Socks5AuthMethod authMethod()
  {
    return authMethod;
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder(StringUtil.simpleClassName(this));
    
    DecoderResult decoderResult = decoderResult();
    if (!decoderResult.isSuccess()) {
      buf.append("(decoderResult: ");
      buf.append(decoderResult);
      buf.append(", authMethod: ");
    } else {
      buf.append("(authMethod: ");
    }
    buf.append(authMethod());
    buf.append(')');
    
    return buf.toString();
  }
}
