package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;



















public final class ResourceLeakDetector<T>
{
  private static final String PROP_LEVEL = "io.netty.leakDetectionLevel";
  private static final Level DEFAULT_LEVEL = Level.SIMPLE;
  

  private static Level level;
  


  public static enum Level
  {
    DISABLED, 
    



    SIMPLE, 
    



    ADVANCED, 
    



    PARANOID;
    
    private Level() {}
  }
  
  private static final InternalLogger logger = InternalLoggerFactory.getInstance(ResourceLeakDetector.class);
  private static final int DEFAULT_SAMPLING_INTERVAL = 113;
  
  static { boolean disabled;
    if (SystemPropertyUtil.get("io.netty.noResourceLeakDetection") != null) {
      boolean disabled = SystemPropertyUtil.getBoolean("io.netty.noResourceLeakDetection", false);
      logger.debug("-Dio.netty.noResourceLeakDetection: {}", Boolean.valueOf(disabled));
      logger.warn("-Dio.netty.noResourceLeakDetection is deprecated. Use '-D{}={}' instead.", "io.netty.leakDetectionLevel", DEFAULT_LEVEL.name().toLowerCase());
    }
    else
    {
      disabled = false;
    }
    
    Level defaultLevel = disabled ? Level.DISABLED : DEFAULT_LEVEL;
    String levelStr = SystemPropertyUtil.get("io.netty.leakDetectionLevel", defaultLevel.name()).trim().toUpperCase();
    Level level = DEFAULT_LEVEL;
    for (Level l : EnumSet.allOf(Level.class)) {
      if ((levelStr.equals(l.name())) || (levelStr.equals(String.valueOf(l.ordinal())))) {
        level = l;
      }
    }
    
    level = level;
    if (logger.isDebugEnabled()) {
      logger.debug("-D{}: {}", "io.netty.leakDetectionLevel", level.name().toLowerCase());
    }
  }
  




  @Deprecated
  public static void setEnabled(boolean enabled)
  {
    setLevel(enabled ? Level.SIMPLE : Level.DISABLED);
  }
  


  public static boolean isEnabled()
  {
    return getLevel().ordinal() > Level.DISABLED.ordinal();
  }
  


  public static void setLevel(Level level)
  {
    if (level == null) {
      throw new NullPointerException("level");
    }
    level = level;
  }
  


  public static Level getLevel()
  {
    return level;
  }
  

  private final ResourceLeakDetector<T>.DefaultResourceLeak head = new DefaultResourceLeak(null);
  private final ResourceLeakDetector<T>.DefaultResourceLeak tail = new DefaultResourceLeak(null);
  
  private final ReferenceQueue<Object> refQueue = new ReferenceQueue();
  private final ConcurrentMap<String, Boolean> reportedLeaks = PlatformDependent.newConcurrentHashMap();
  
  private final String resourceType;
  private final int samplingInterval;
  private final long maxActive;
  private long active;
  private final AtomicBoolean loggedTooManyActive = new AtomicBoolean();
  private long leakCheckCnt;
  
  public ResourceLeakDetector(Class<?> resourceType)
  {
    this(StringUtil.simpleClassName(resourceType));
  }
  
  public ResourceLeakDetector(String resourceType) {
    this(resourceType, 113, Long.MAX_VALUE);
  }
  
  public ResourceLeakDetector(Class<?> resourceType, int samplingInterval, long maxActive) {
    this(StringUtil.simpleClassName(resourceType), samplingInterval, maxActive);
  }
  
  public ResourceLeakDetector(String resourceType, int samplingInterval, long maxActive) {
    if (resourceType == null) {
      throw new NullPointerException("resourceType");
    }
    if (samplingInterval <= 0) {
      throw new IllegalArgumentException("samplingInterval: " + samplingInterval + " (expected: 1+)");
    }
    if (maxActive <= 0L) {
      throw new IllegalArgumentException("maxActive: " + maxActive + " (expected: 1+)");
    }
    
    this.resourceType = resourceType;
    this.samplingInterval = samplingInterval;
    this.maxActive = maxActive;
    
    head.next = tail;
    tail.prev = head;
  }
  





  public ResourceLeak open(T obj)
  {
    Level level = level;
    if (level == Level.DISABLED) {
      return null;
    }
    
    if (level.ordinal() < Level.PARANOID.ordinal()) {
      if (leakCheckCnt++ % samplingInterval == 0L) {
        reportLeak(level);
        return new DefaultResourceLeak(obj);
      }
      return null;
    }
    
    reportLeak(level);
    return new DefaultResourceLeak(obj);
  }
  
  private void reportLeak(Level level)
  {
    if (!logger.isErrorEnabled())
    {
      for (;;) {
        ResourceLeakDetector<T>.DefaultResourceLeak ref = (DefaultResourceLeak)refQueue.poll();
        if (ref == null) {
          break;
        }
        ref.close();
      }
      return;
    }
    

    int samplingInterval = level == Level.PARANOID ? 1 : this.samplingInterval;
    if ((active * samplingInterval > maxActive) && (loggedTooManyActive.compareAndSet(false, true))) {
      logger.error("LEAK: You are creating too many " + resourceType + " instances.  " + resourceType + " is a shared resource that must be reused across the JVM," + "so that only a few instances are created.");
    }
    



    for (;;)
    {
      ResourceLeakDetector<T>.DefaultResourceLeak ref = (DefaultResourceLeak)refQueue.poll();
      if (ref == null) {
        break;
      }
      
      ref.clear();
      
      if (ref.close())
      {


        String records = ref.toString();
        if (reportedLeaks.putIfAbsent(records, Boolean.TRUE) == null) {
          if (records.isEmpty()) {
            logger.error("LEAK: {}.release() was not called before it's garbage-collected. Enable advanced leak reporting to find out where the leak occurred. To enable advanced leak reporting, specify the JVM option '-D{}={}' or call {}.setLevel()", new Object[] { resourceType, "io.netty.leakDetectionLevel", Level.ADVANCED.name().toLowerCase(), StringUtil.simpleClassName(this) });

          }
          else
          {

            logger.error("LEAK: {}.release() was not called before it's garbage-collected.{}", resourceType, records);
          }
        }
      }
    }
  }
  
  private final class DefaultResourceLeak
    extends PhantomReference<Object>
    implements ResourceLeak
  {
    private static final int MAX_RECORDS = 4;
    private final String creationRecord;
    private final Deque<String> lastRecords = new ArrayDeque();
    private final AtomicBoolean freed;
    private ResourceLeakDetector<T>.DefaultResourceLeak prev;
    private ResourceLeakDetector<T>.DefaultResourceLeak next;
    
    DefaultResourceLeak(Object referent) {
      super(referent != null ? refQueue : null);
      ResourceLeakDetector.Level level;
      if (referent != null) {
        level = ResourceLeakDetector.getLevel();
        if (level.ordinal() >= ResourceLeakDetector.Level.ADVANCED.ordinal()) {
          creationRecord = ResourceLeakDetector.newRecord(3);
        } else {
          creationRecord = null;
        }
        

        synchronized (head) {
          prev = head;
          next = head.next;
          head.next.prev = this;
          head.next = this;
          ResourceLeakDetector.access$408(ResourceLeakDetector.this);
        }
        freed = new AtomicBoolean();
      } else {
        creationRecord = null;
        freed = new AtomicBoolean(true);
      }
    }
    
    public void record()
    {
      if (creationRecord != null) {
        String value = ResourceLeakDetector.newRecord(2);
        
        synchronized (lastRecords) {
          int size = lastRecords.size();
          if ((size == 0) || (!((String)lastRecords.getLast()).equals(value))) {
            lastRecords.add(value);
          }
          if (size > 4) {
            lastRecords.removeFirst();
          }
        }
      }
    }
    
    public boolean close()
    {
      if (freed.compareAndSet(false, true)) {
        synchronized (head) {
          ResourceLeakDetector.access$410(ResourceLeakDetector.this);
          prev.next = next;
          next.prev = prev;
          prev = null;
          next = null;
        }
        return true;
      }
      return false;
    }
    
    public String toString() {
      if (creationRecord == null) {
        return "";
      }
      
      Object[] array;
      synchronized (lastRecords) {
        array = lastRecords.toArray();
      }
      
      StringBuilder buf = new StringBuilder(16384);
      buf.append(StringUtil.NEWLINE);
      buf.append("Recent access records: ");
      buf.append(array.length);
      buf.append(StringUtil.NEWLINE);
      
      if (array.length > 0) {
        for (int i = array.length - 1; i >= 0; i--) {
          buf.append('#');
          buf.append(i + 1);
          buf.append(':');
          buf.append(StringUtil.NEWLINE);
          buf.append(array[i]);
        }
      }
      
      buf.append("Created at:");
      buf.append(StringUtil.NEWLINE);
      buf.append(creationRecord);
      buf.setLength(buf.length() - StringUtil.NEWLINE.length());
      
      return buf.toString();
    }
  }
  
  private static final String[] STACK_TRACE_ELEMENT_EXCLUSIONS = { "io.netty.buffer.AbstractByteBufAllocator.toLeakAwareBuffer(" };
  

  static String newRecord(int recordsToSkip)
  {
    StringBuilder buf = new StringBuilder(4096);
    StackTraceElement[] array = new Throwable().getStackTrace();
    for (StackTraceElement e : array) {
      if (recordsToSkip > 0) {
        recordsToSkip--;
      } else {
        String estr = e.toString();
        

        boolean excluded = false;
        for (String exclusion : STACK_TRACE_ELEMENT_EXCLUSIONS) {
          if (estr.startsWith(exclusion)) {
            excluded = true;
            break;
          }
        }
        
        if (!excluded) {
          buf.append('\t');
          buf.append(estr);
          buf.append(StringUtil.NEWLINE);
        }
      }
    }
    
    return buf.toString();
  }
}
