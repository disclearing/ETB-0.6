package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;























public abstract class SocksMessage
{
  private final SocksMessageType type;
  private final SocksProtocolVersion protocolVersion = SocksProtocolVersion.SOCKS5;
  
  protected SocksMessage(SocksMessageType type) {
    if (type == null) {
      throw new NullPointerException("type");
    }
    this.type = type;
  }
  




  public SocksMessageType type()
  {
    return type;
  }
  




  public SocksProtocolVersion protocolVersion()
  {
    return protocolVersion;
  }
  
  @Deprecated
  public abstract void encodeAsByteBuf(ByteBuf paramByteBuf);
}
