package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.util.internal.PlatformDependent;
import java.util.Map;



















public final class EpollSocketChannelConfig
  extends DefaultChannelConfig
  implements SocketChannelConfig
{
  private final EpollSocketChannel channel;
  private volatile boolean allowHalfClosure;
  
  EpollSocketChannelConfig(EpollSocketChannel channel)
  {
    super(channel);
    
    this.channel = channel;
    if (PlatformDependent.canEnableTcpNoDelayByDefault()) {
      setTcpNoDelay(true);
    }
  }
  
  public Map<ChannelOption<?>, Object> getOptions()
  {
    return getOptions(super.getOptions(), new ChannelOption[] { ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.TCP_NODELAY, ChannelOption.SO_KEEPALIVE, ChannelOption.SO_REUSEADDR, ChannelOption.SO_LINGER, ChannelOption.IP_TOS, ChannelOption.ALLOW_HALF_CLOSURE, EpollChannelOption.TCP_CORK, EpollChannelOption.TCP_KEEPCNT, EpollChannelOption.TCP_KEEPIDLE, EpollChannelOption.TCP_KEEPINTVL });
  }
  





  public <T> T getOption(ChannelOption<T> option)
  {
    if (option == ChannelOption.SO_RCVBUF) {
      return Integer.valueOf(getReceiveBufferSize());
    }
    if (option == ChannelOption.SO_SNDBUF) {
      return Integer.valueOf(getSendBufferSize());
    }
    if (option == ChannelOption.TCP_NODELAY) {
      return Boolean.valueOf(isTcpNoDelay());
    }
    if (option == ChannelOption.SO_KEEPALIVE) {
      return Boolean.valueOf(isKeepAlive());
    }
    if (option == ChannelOption.SO_REUSEADDR) {
      return Boolean.valueOf(isReuseAddress());
    }
    if (option == ChannelOption.SO_LINGER) {
      return Integer.valueOf(getSoLinger());
    }
    if (option == ChannelOption.IP_TOS) {
      return Integer.valueOf(getTrafficClass());
    }
    if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
      return Boolean.valueOf(isAllowHalfClosure());
    }
    if (option == EpollChannelOption.TCP_CORK) {
      return Boolean.valueOf(isTcpCork());
    }
    if (option == EpollChannelOption.TCP_KEEPIDLE) {
      return Integer.valueOf(getTcpKeepIdle());
    }
    if (option == EpollChannelOption.TCP_KEEPINTVL) {
      return Integer.valueOf(getTcpKeepIntvl());
    }
    if (option == EpollChannelOption.TCP_KEEPCNT) {
      return Integer.valueOf(getTcpKeepCnt());
    }
    return super.getOption(option);
  }
  
  public <T> boolean setOption(ChannelOption<T> option, T value)
  {
    validate(option, value);
    
    if (option == ChannelOption.SO_RCVBUF) {
      setReceiveBufferSize(((Integer)value).intValue());
    } else if (option == ChannelOption.SO_SNDBUF) {
      setSendBufferSize(((Integer)value).intValue());
    } else if (option == ChannelOption.TCP_NODELAY) {
      setTcpNoDelay(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.SO_KEEPALIVE) {
      setKeepAlive(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.SO_REUSEADDR) {
      setReuseAddress(((Boolean)value).booleanValue());
    } else if (option == ChannelOption.SO_LINGER) {
      setSoLinger(((Integer)value).intValue());
    } else if (option == ChannelOption.IP_TOS) {
      setTrafficClass(((Integer)value).intValue());
    } else if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
      setAllowHalfClosure(((Boolean)value).booleanValue());
    } else if (option == EpollChannelOption.TCP_CORK) {
      setTcpCork(((Boolean)value).booleanValue());
    } else if (option == EpollChannelOption.TCP_KEEPIDLE) {
      setTcpKeepIdle(((Integer)value).intValue());
    } else if (option == EpollChannelOption.TCP_KEEPCNT) {
      setTcpKeepCntl(((Integer)value).intValue());
    } else if (option == EpollChannelOption.TCP_KEEPINTVL) {
      setTcpKeepIntvl(((Integer)value).intValue());
    } else {
      return super.setOption(option, value);
    }
    
    return true;
  }
  
  public int getReceiveBufferSize()
  {
    return Native.getReceiveBufferSize(channel.fd);
  }
  
  public int getSendBufferSize()
  {
    return Native.getSendBufferSize(channel.fd);
  }
  
  public int getSoLinger()
  {
    return Native.getSoLinger(channel.fd);
  }
  
  public int getTrafficClass()
  {
    return Native.getTrafficClass(channel.fd);
  }
  
  public boolean isKeepAlive()
  {
    return Native.isKeepAlive(channel.fd) == 1;
  }
  
  public boolean isReuseAddress()
  {
    return Native.isReuseAddress(channel.fd) == 1;
  }
  
  public boolean isTcpNoDelay()
  {
    return Native.isTcpNoDelay(channel.fd) == 1;
  }
  


  public boolean isTcpCork()
  {
    return Native.isTcpCork(channel.fd) == 1;
  }
  


  public int getTcpKeepIdle()
  {
    return Native.getTcpKeepIdle(channel.fd);
  }
  


  public int getTcpKeepIntvl()
  {
    return Native.getTcpKeepIntvl(channel.fd);
  }
  


  public int getTcpKeepCnt()
  {
    return Native.getTcpKeepCnt(channel.fd);
  }
  
  public EpollSocketChannelConfig setKeepAlive(boolean keepAlive)
  {
    Native.setKeepAlive(channel.fd, keepAlive ? 1 : 0);
    return this;
  }
  

  public EpollSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth)
  {
    return this;
  }
  
  public EpollSocketChannelConfig setReceiveBufferSize(int receiveBufferSize)
  {
    Native.setReceiveBufferSize(channel.fd, receiveBufferSize);
    return this;
  }
  
  public EpollSocketChannelConfig setReuseAddress(boolean reuseAddress)
  {
    Native.setReuseAddress(channel.fd, reuseAddress ? 1 : 0);
    return this;
  }
  
  public EpollSocketChannelConfig setSendBufferSize(int sendBufferSize)
  {
    Native.setSendBufferSize(channel.fd, sendBufferSize);
    return this;
  }
  
  public EpollSocketChannelConfig setSoLinger(int soLinger)
  {
    Native.setSoLinger(channel.fd, soLinger);
    return this;
  }
  
  public EpollSocketChannelConfig setTcpNoDelay(boolean tcpNoDelay)
  {
    Native.setTcpNoDelay(channel.fd, tcpNoDelay ? 1 : 0);
    return this;
  }
  


  public EpollSocketChannelConfig setTcpCork(boolean tcpCork)
  {
    Native.setTcpCork(channel.fd, tcpCork ? 1 : 0);
    return this;
  }
  
  public EpollSocketChannelConfig setTrafficClass(int trafficClass)
  {
    Native.setTrafficClass(channel.fd, trafficClass);
    return this;
  }
  


  public EpollSocketChannelConfig setTcpKeepIdle(int seconds)
  {
    Native.setTcpKeepIdle(channel.fd, seconds);
    return this;
  }
  


  public EpollSocketChannelConfig setTcpKeepIntvl(int seconds)
  {
    Native.setTcpKeepIntvl(channel.fd, seconds);
    return this;
  }
  


  public EpollSocketChannelConfig setTcpKeepCntl(int probes)
  {
    Native.setTcpKeepCnt(channel.fd, probes);
    return this;
  }
  
  public boolean isAllowHalfClosure()
  {
    return allowHalfClosure;
  }
  
  public EpollSocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure)
  {
    this.allowHalfClosure = allowHalfClosure;
    return this;
  }
  
  public EpollSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis)
  {
    super.setConnectTimeoutMillis(connectTimeoutMillis);
    return this;
  }
  
  public EpollSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead)
  {
    super.setMaxMessagesPerRead(maxMessagesPerRead);
    return this;
  }
  
  public EpollSocketChannelConfig setWriteSpinCount(int writeSpinCount)
  {
    super.setWriteSpinCount(writeSpinCount);
    return this;
  }
  
  public EpollSocketChannelConfig setAllocator(ByteBufAllocator allocator)
  {
    super.setAllocator(allocator);
    return this;
  }
  
  public EpollSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator)
  {
    super.setRecvByteBufAllocator(allocator);
    return this;
  }
  
  public EpollSocketChannelConfig setAutoRead(boolean autoRead)
  {
    super.setAutoRead(autoRead);
    return this;
  }
  
  public EpollSocketChannelConfig setAutoClose(boolean autoClose)
  {
    super.setAutoClose(autoClose);
    return this;
  }
  
  public EpollSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark)
  {
    super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    return this;
  }
  
  public EpollSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark)
  {
    super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    return this;
  }
  
  public EpollSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator)
  {
    super.setMessageSizeEstimator(estimator);
    return this;
  }
  
  protected void autoReadCleared()
  {
    channel.clearEpollIn();
  }
}
