package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.Channel.Unsafe;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

















public final class ServerBootstrap
  extends AbstractBootstrap<ServerBootstrap, ServerChannel>
{
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);
  
  private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap();
  private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap();
  private volatile EventLoopGroup childGroup;
  private volatile ChannelHandler childHandler;
  
  public ServerBootstrap() {}
  
  private ServerBootstrap(ServerBootstrap bootstrap) {
    super(bootstrap);
    childGroup = childGroup;
    childHandler = childHandler;
    synchronized (childOptions) {
      childOptions.putAll(childOptions);
    }
    synchronized (childAttrs) {
      childAttrs.putAll(childAttrs);
    }
  }
  



  public ServerBootstrap group(EventLoopGroup group)
  {
    return group(group, group);
  }
  




  public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup)
  {
    super.group(parentGroup);
    if (childGroup == null) {
      throw new NullPointerException("childGroup");
    }
    if (this.childGroup != null) {
      throw new IllegalStateException("childGroup set already");
    }
    this.childGroup = childGroup;
    return this;
  }
  




  public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value)
  {
    if (childOption == null) {
      throw new NullPointerException("childOption");
    }
    if (value == null) {
      synchronized (childOptions) {
        childOptions.remove(childOption);
      }
    } else {
      synchronized (childOptions) {
        childOptions.put(childOption, value);
      }
    }
    return this;
  }
  



  public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value)
  {
    if (childKey == null) {
      throw new NullPointerException("childKey");
    }
    if (value == null) {
      childAttrs.remove(childKey);
    } else {
      childAttrs.put(childKey, value);
    }
    return this;
  }
  


  public ServerBootstrap childHandler(ChannelHandler childHandler)
  {
    if (childHandler == null) {
      throw new NullPointerException("childHandler");
    }
    this.childHandler = childHandler;
    return this;
  }
  



  public EventLoopGroup childGroup()
  {
    return childGroup;
  }
  
  void init(Channel channel) throws Exception
  {
    Map<ChannelOption<?>, Object> options = options();
    synchronized (options) {
      channel.config().setOptions(options);
    }
    
    Map<AttributeKey<?>, Object> attrs = attrs();
    synchronized (attrs) {
      for (Map.Entry<AttributeKey<?>, Object> e : attrs.entrySet())
      {
        AttributeKey<Object> key = (AttributeKey)e.getKey();
        channel.attr(key).set(e.getValue());
      }
    }
    
    ChannelPipeline p = channel.pipeline();
    if (handler() != null) {
      p.addLast(new ChannelHandler[] { handler() });
    }
    
    final EventLoopGroup currentChildGroup = childGroup;
    final ChannelHandler currentChildHandler = childHandler;
    
    final Map.Entry<ChannelOption<?>, Object>[] currentChildOptions;
    synchronized (childOptions) {
      currentChildOptions = (Map.Entry[])childOptions.entrySet().toArray(newOptionArray(childOptions.size())); }
    Object currentChildAttrs;
    synchronized (childAttrs) {
      currentChildAttrs = (Map.Entry[])childAttrs.entrySet().toArray(newAttrArray(childAttrs.size()));
    }
    
    p.addLast(new ChannelHandler[] { new ChannelInitializer()
    {
      public void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new ChannelHandler[] { new ServerBootstrap.ServerBootstrapAcceptor(currentChildGroup, currentChildHandler, currentChildOptions, val$currentChildAttrs) });
      }
    } });
  }
  

  public ServerBootstrap validate()
  {
    super.validate();
    if (childHandler == null) {
      throw new IllegalStateException("childHandler not set");
    }
    if (childGroup == null) {
      logger.warn("childGroup is not set. Using parentGroup instead.");
      childGroup = group();
    }
    return this;
  }
  
  private static Map.Entry<ChannelOption<?>, Object>[] newOptionArray(int size)
  {
    return new Map.Entry[size];
  }
  
  private static Map.Entry<AttributeKey<?>, Object>[] newAttrArray(int size)
  {
    return new Map.Entry[size];
  }
  
  private static class ServerBootstrapAcceptor
    extends ChannelInboundHandlerAdapter
  {
    private final EventLoopGroup childGroup;
    private final ChannelHandler childHandler;
    private final Map.Entry<ChannelOption<?>, Object>[] childOptions;
    private final Map.Entry<AttributeKey<?>, Object>[] childAttrs;
    
    ServerBootstrapAcceptor(EventLoopGroup childGroup, ChannelHandler childHandler, Map.Entry<ChannelOption<?>, Object>[] childOptions, Map.Entry<AttributeKey<?>, Object>[] childAttrs)
    {
      this.childGroup = childGroup;
      this.childHandler = childHandler;
      this.childOptions = childOptions;
      this.childAttrs = childAttrs;
    }
    

    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
      final Channel child = (Channel)msg;
      
      child.pipeline().addLast(new ChannelHandler[] { childHandler });
      
      for (Map.Entry<ChannelOption<?>, Object> e : childOptions) {
        try {
          if (!child.config().setOption((ChannelOption)e.getKey(), e.getValue())) {
            ServerBootstrap.logger.warn("Unknown channel option: " + e);
          }
        } catch (Throwable t) {
          ServerBootstrap.logger.warn("Failed to set a channel option: " + child, t);
        }
      }
      
      for (Map.Entry<AttributeKey<?>, Object> e : childAttrs) {
        child.attr((AttributeKey)e.getKey()).set(e.getValue());
      }
      try
      {
        childGroup.register(child).addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              ServerBootstrap.ServerBootstrapAcceptor.forceClose(child, future.cause());
            }
          }
        });
      } catch (Throwable t) {
        forceClose(child, t);
      }
    }
    
    private static void forceClose(Channel child, Throwable t) {
      child.unsafe().closeForcibly();
      ServerBootstrap.logger.warn("Failed to register an accepted channel: " + child, t);
    }
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
      final ChannelConfig config = ctx.channel().config();
      if (config.isAutoRead())
      {

        config.setAutoRead(false);
        ctx.channel().eventLoop().schedule(new Runnable()
        {

          public void run() { config.setAutoRead(true); } }, 1L, TimeUnit.SECONDS);
      }
      



      ctx.fireExceptionCaught(cause);
    }
  }
  

  public ServerBootstrap clone()
  {
    return new ServerBootstrap(this);
  }
  
  public String toString()
  {
    StringBuilder buf = new StringBuilder(super.toString());
    buf.setLength(buf.length() - 1);
    buf.append(", ");
    if (childGroup != null) {
      buf.append("childGroup: ");
      buf.append(StringUtil.simpleClassName(childGroup));
      buf.append(", ");
    }
    synchronized (childOptions) {
      if (!childOptions.isEmpty()) {
        buf.append("childOptions: ");
        buf.append(childOptions);
        buf.append(", ");
      }
    }
    synchronized (childAttrs) {
      if (!childAttrs.isEmpty()) {
        buf.append("childAttrs: ");
        buf.append(childAttrs);
        buf.append(", ");
      }
    }
    if (childHandler != null) {
      buf.append("childHandler: ");
      buf.append(childHandler);
      buf.append(", ");
    }
    if (buf.charAt(buf.length() - 1) == '(') {
      buf.append(')');
    } else {
      buf.setCharAt(buf.length() - 2, ')');
      buf.setLength(buf.length() - 1);
    }
    
    return buf.toString();
  }
}
