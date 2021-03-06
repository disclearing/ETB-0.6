package io.netty.handler.codec.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageLite.Builder;
import com.google.protobuf.Parser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;














































@ChannelHandler.Sharable
public class ProtobufDecoder
  extends MessageToMessageDecoder<ByteBuf>
{
  private static final boolean HAS_PARSER;
  private final MessageLite prototype;
  private final ExtensionRegistry extensionRegistry;
  
  static
  {
    boolean hasParser = false;
    try
    {
      MessageLite.class.getDeclaredMethod("getParserForType", new Class[0]);
      hasParser = true;
    }
    catch (Throwable t) {}
    

    HAS_PARSER = hasParser;
  }
  





  public ProtobufDecoder(MessageLite prototype)
  {
    this(prototype, null);
  }
  
  public ProtobufDecoder(MessageLite prototype, ExtensionRegistry extensionRegistry) {
    if (prototype == null) {
      throw new NullPointerException("prototype");
    }
    this.prototype = prototype.getDefaultInstanceForType();
    this.extensionRegistry = extensionRegistry;
  }
  

  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
    throws Exception
  {
    int length = msg.readableBytes();
    int offset; byte[] array; int offset; if (msg.hasArray()) {
      byte[] array = msg.array();
      offset = msg.arrayOffset() + msg.readerIndex();
    } else {
      array = new byte[length];
      msg.getBytes(msg.readerIndex(), array, 0, length);
      offset = 0;
    }
    
    if (extensionRegistry == null) {
      if (HAS_PARSER) {
        out.add(prototype.getParserForType().parseFrom(array, offset, length));
      } else {
        out.add(prototype.newBuilderForType().mergeFrom(array, offset, length).build());
      }
    }
    else if (HAS_PARSER) {
      out.add(prototype.getParserForType().parseFrom(array, offset, length, extensionRegistry));
    } else {
      out.add(prototype.newBuilderForType().mergeFrom(array, offset, length, extensionRegistry).build());
    }
  }
}
