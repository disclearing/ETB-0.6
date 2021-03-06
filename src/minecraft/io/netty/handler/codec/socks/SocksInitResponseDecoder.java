package io.netty.handler.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.List;



















public class SocksInitResponseDecoder
  extends ReplayingDecoder<State>
{
  private static final String name = "SOCKS_INIT_RESPONSE_DECODER";
  private SocksProtocolVersion version;
  private SocksAuthScheme authScheme;
  
  @Deprecated
  public static String getName()
  {
    return "SOCKS_INIT_RESPONSE_DECODER";
  }
  



  private SocksResponse msg = SocksCommonUtils.UNKNOWN_SOCKS_RESPONSE;
  
  public SocksInitResponseDecoder() {
    super(State.CHECK_PROTOCOL_VERSION);
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$socks$SocksInitResponseDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      version = SocksProtocolVersion.valueOf(byteBuf.readByte());
      if (version == SocksProtocolVersion.SOCKS5)
      {

        checkpoint(State.READ_PREFFERED_AUTH_TYPE); }
      break;
    case 2: 
      authScheme = SocksAuthScheme.valueOf(byteBuf.readByte());
      msg = new SocksInitResponse(authScheme);
    }
    
    
    ctx.pipeline().remove(this);
    out.add(msg);
  }
  
  static enum State {
    CHECK_PROTOCOL_VERSION, 
    READ_PREFFERED_AUTH_TYPE;
    
    private State() {}
  }
}
