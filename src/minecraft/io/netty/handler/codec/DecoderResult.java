package io.netty.handler.codec;

import io.netty.util.Signal;
















public class DecoderResult
{
  protected static final Signal SIGNAL_UNFINISHED = Signal.valueOf(DecoderResult.class.getName() + ".UNFINISHED");
  protected static final Signal SIGNAL_SUCCESS = Signal.valueOf(DecoderResult.class.getName() + ".SUCCESS");
  
  public static final DecoderResult UNFINISHED = new DecoderResult(SIGNAL_UNFINISHED);
  public static final DecoderResult SUCCESS = new DecoderResult(SIGNAL_SUCCESS);
  private final Throwable cause;
  
  public static DecoderResult failure(Throwable cause) { if (cause == null) {
      throw new NullPointerException("cause");
    }
    return new DecoderResult(cause);
  }
  

  protected DecoderResult(Throwable cause)
  {
    if (cause == null) {
      throw new NullPointerException("cause");
    }
    this.cause = cause;
  }
  
  public boolean isFinished() {
    return cause != SIGNAL_UNFINISHED;
  }
  
  public boolean isSuccess() {
    return cause == SIGNAL_SUCCESS;
  }
  
  public boolean isFailure() {
    return (cause != SIGNAL_SUCCESS) && (cause != SIGNAL_UNFINISHED);
  }
  
  public Throwable cause() {
    if (isFailure()) {
      return cause;
    }
    return null;
  }
  

  public String toString()
  {
    if (isFinished()) {
      if (isSuccess()) {
        return "success";
      }
      
      String cause = cause().toString();
      StringBuilder buf = new StringBuilder(cause.length() + 17);
      buf.append("failure(");
      buf.append(cause);
      buf.append(')');
      
      return buf.toString();
    }
    return "unfinished";
  }
}
