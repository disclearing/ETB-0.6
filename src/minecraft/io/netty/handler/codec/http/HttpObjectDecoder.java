package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;
import java.util.List;


























































































public abstract class HttpObjectDecoder
  extends ReplayingDecoder<State>
{
  private final int maxInitialLineLength;
  private final int maxHeaderSize;
  private final int maxChunkSize;
  private final boolean chunkedSupported;
  protected final boolean validateHeaders;
  private final AppendableCharSequence seq = new AppendableCharSequence(128);
  private final HeaderParser headerParser = new HeaderParser(seq);
  private final LineParser lineParser = new LineParser(seq);
  
  private HttpMessage message;
  private long chunkSize;
  private int headerSize;
  private long contentLength = Long.MIN_VALUE;
  



  static enum State
  {
    SKIP_CONTROL_CHARS, 
    READ_INITIAL, 
    READ_HEADER, 
    READ_VARIABLE_LENGTH_CONTENT, 
    READ_FIXED_LENGTH_CONTENT, 
    READ_CHUNK_SIZE, 
    READ_CHUNKED_CONTENT, 
    READ_CHUNK_DELIMITER, 
    READ_CHUNK_FOOTER, 
    BAD_MESSAGE, 
    UPGRADED;
    

    private State() {}
  }
  

  protected HttpObjectDecoder()
  {
    this(4096, 8192, 8192, true);
  }
  



  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported)
  {
    this(maxInitialLineLength, maxHeaderSize, maxChunkSize, chunkedSupported, true);
  }
  





  protected HttpObjectDecoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean chunkedSupported, boolean validateHeaders)
  {
    super(State.SKIP_CONTROL_CHARS);
    
    if (maxInitialLineLength <= 0) {
      throw new IllegalArgumentException("maxInitialLineLength must be a positive integer: " + maxInitialLineLength);
    }
    

    if (maxHeaderSize <= 0) {
      throw new IllegalArgumentException("maxHeaderSize must be a positive integer: " + maxHeaderSize);
    }
    

    if (maxChunkSize <= 0) {
      throw new IllegalArgumentException("maxChunkSize must be a positive integer: " + maxChunkSize);
    }
    

    this.maxInitialLineLength = maxInitialLineLength;
    this.maxHeaderSize = maxHeaderSize;
    this.maxChunkSize = maxChunkSize;
    this.chunkedSupported = chunkedSupported;
    this.validateHeaders = validateHeaders;
  }
  
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception
  {
    switch (1.$SwitchMap$io$netty$handler$codec$http$HttpObjectDecoder$State[((State)state()).ordinal()]) {
    case 1: 
      try {
        skipControlCharacters(buffer);
        checkpoint(State.READ_INITIAL);
      } finally {
        checkpoint();
      }
    case 2: 
      try {
        String[] initialLine = splitInitialLine(lineParser.parse(buffer));
        if (initialLine.length < 3)
        {
          checkpoint(State.SKIP_CONTROL_CHARS);
          return;
        }
        
        message = createMessage(initialLine);
        checkpoint(State.READ_HEADER);
      }
      catch (Exception e) {
        out.add(invalidMessage(e));
        return;
      }
    case 3:  try {
        State nextState = readHeaders(buffer);
        checkpoint(nextState);
        if (nextState == State.READ_CHUNK_SIZE) {
          if (!chunkedSupported) {
            throw new IllegalArgumentException("Chunked messages not supported");
          }
          
          out.add(message);
          return;
        }
        if (nextState == State.SKIP_CONTROL_CHARS)
        {
          out.add(message);
          out.add(LastHttpContent.EMPTY_LAST_CONTENT);
          reset();
          return;
        }
        long contentLength = contentLength();
        if ((contentLength == 0L) || ((contentLength == -1L) && (isDecodingRequest()))) {
          out.add(message);
          out.add(LastHttpContent.EMPTY_LAST_CONTENT);
          reset();
          return;
        }
        
        assert ((nextState == State.READ_FIXED_LENGTH_CONTENT) || (nextState == State.READ_VARIABLE_LENGTH_CONTENT));
        
        out.add(message);
        
        if (nextState == State.READ_FIXED_LENGTH_CONTENT)
        {
          this.chunkSize = contentLength;
        }
        

        return;
      } catch (Exception e) {
        out.add(invalidMessage(e));
        return;
      }
    
    case 4: 
      int toRead = Math.min(actualReadableBytes(), maxChunkSize);
      if (toRead > 0) {
        ByteBuf content = ByteBufUtil.readBytes(ctx.alloc(), buffer, toRead);
        if (buffer.isReadable()) {
          out.add(new DefaultHttpContent(content));
        }
        else {
          out.add(new DefaultLastHttpContent(content, validateHeaders));
          reset();
        }
      } else if (!buffer.isReadable())
      {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
        reset();
      }
      return;
    
    case 5: 
      int readLimit = actualReadableBytes();
      






      if (readLimit == 0) {
        return;
      }
      
      int toRead = Math.min(readLimit, maxChunkSize);
      if (toRead > this.chunkSize) {
        toRead = (int)this.chunkSize;
      }
      ByteBuf content = ByteBufUtil.readBytes(ctx.alloc(), buffer, toRead);
      this.chunkSize -= toRead;
      
      if (this.chunkSize == 0L)
      {
        out.add(new DefaultLastHttpContent(content, validateHeaders));
        reset();
      } else {
        out.add(new DefaultHttpContent(content));
      }
      return;
    


    case 6: 
      try
      {
        AppendableCharSequence line = lineParser.parse(buffer);
        int chunkSize = getChunkSize(line.toString());
        this.chunkSize = chunkSize;
        if (chunkSize == 0) {
          checkpoint(State.READ_CHUNK_FOOTER);
          return;
        }
        checkpoint(State.READ_CHUNKED_CONTENT);
      }
      catch (Exception e) {
        out.add(invalidChunk(e));
        return;
      }
    case 7: 
      assert (this.chunkSize <= 2147483647L);
      int toRead = Math.min((int)this.chunkSize, maxChunkSize);
      
      HttpContent chunk = new DefaultHttpContent(ByteBufUtil.readBytes(ctx.alloc(), buffer, toRead));
      this.chunkSize -= toRead;
      
      out.add(chunk);
      
      if (this.chunkSize == 0L)
      {
        checkpoint(State.READ_CHUNK_DELIMITER);
      } else {
        return;
      }
    case 8: 
      for (;;)
      {
        byte next = buffer.readByte();
        if (next == 13) {
          if (buffer.readByte() == 10) {
            checkpoint(State.READ_CHUNK_SIZE);
          }
        } else {
          if (next == 10) {
            checkpoint(State.READ_CHUNK_SIZE);
            return;
          }
          checkpoint();
        }
      }
    case 9: 
      try {
        LastHttpContent trailer = readTrailingHeaders(buffer);
        out.add(trailer);
        reset();
        return;
      } catch (Exception e) {
        out.add(invalidChunk(e));
        return;
      }
    
    case 10: 
      buffer.skipBytes(actualReadableBytes());
      break;
    
    case 11: 
      int readableBytes = actualReadableBytes();
      if (readableBytes > 0)
      {



        out.add(buffer.readBytes(actualReadableBytes()));
      }
      break;
    }
  }
  
  protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
    throws Exception
  {
    decode(ctx, in, out);
    

    if (message != null)
    {
      boolean prematureClosure;
      boolean prematureClosure;
      if (isDecodingRequest())
      {
        prematureClosure = true;

      }
      else
      {
        prematureClosure = contentLength() > 0L;
      }
      reset();
      
      if (!prematureClosure) {
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      }
    }
  }
  
  protected boolean isContentAlwaysEmpty(HttpMessage msg) {
    if ((msg instanceof HttpResponse)) {
      HttpResponse res = (HttpResponse)msg;
      int code = res.getStatus().code();
      





      if ((code >= 100) && (code < 200))
      {
        return (code != 101) || (res.headers().contains("Sec-WebSocket-Accept"));
      }
      
      switch (code) {
      case 204: case 205: case 304: 
        return true;
      }
    }
    return false;
  }
  
  private void reset() {
    HttpMessage message = this.message;
    this.message = null;
    contentLength = Long.MIN_VALUE;
    if (!isDecodingRequest()) {
      HttpResponse res = (HttpResponse)message;
      if ((res != null) && (res.getStatus().code() == 101)) {
        checkpoint(State.UPGRADED);
        return;
      }
    }
    
    checkpoint(State.SKIP_CONTROL_CHARS);
  }
  
  private HttpMessage invalidMessage(Exception cause) {
    checkpoint(State.BAD_MESSAGE);
    if (message != null) {
      message.setDecoderResult(DecoderResult.failure(cause));
    } else {
      message = createInvalidMessage();
      message.setDecoderResult(DecoderResult.failure(cause));
    }
    
    HttpMessage ret = message;
    message = null;
    return ret;
  }
  
  private HttpContent invalidChunk(Exception cause) {
    checkpoint(State.BAD_MESSAGE);
    HttpContent chunk = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
    chunk.setDecoderResult(DecoderResult.failure(cause));
    message = null;
    return chunk;
  }
  
  private static void skipControlCharacters(ByteBuf buffer) {
    for (;;) {
      char c = (char)buffer.readUnsignedByte();
      if ((!Character.isISOControl(c)) && (!Character.isWhitespace(c)))
      {
        buffer.readerIndex(buffer.readerIndex() - 1);
        break;
      }
    }
  }
  
  private State readHeaders(ByteBuf buffer) {
    headerSize = 0;
    HttpMessage message = this.message;
    HttpHeaders headers = message.headers();
    
    AppendableCharSequence line = headerParser.parse(buffer);
    String name = null;
    String value = null;
    if (line.length() > 0) {
      headers.clear();
      do {
        char firstChar = line.charAt(0);
        if ((name != null) && ((firstChar == ' ') || (firstChar == '\t'))) {
          value = value + ' ' + line.toString().trim();
        } else {
          if (name != null) {
            headers.add(name, value);
          }
          String[] header = splitHeader(line);
          name = header[0];
          value = header[1];
        }
        
        line = headerParser.parse(buffer);
      } while (line.length() > 0);
      

      if (name != null) {
        headers.add(name, value);
      }
    }
    
    State nextState;
    State nextState;
    if (isContentAlwaysEmpty(message)) {
      HttpHeaders.removeTransferEncodingChunked(message);
      nextState = State.SKIP_CONTROL_CHARS; } else { State nextState;
      if (HttpHeaders.isTransferEncodingChunked(message)) {
        nextState = State.READ_CHUNK_SIZE; } else { State nextState;
        if (contentLength() >= 0L) {
          nextState = State.READ_FIXED_LENGTH_CONTENT;
        } else
          nextState = State.READ_VARIABLE_LENGTH_CONTENT;
      } }
    return nextState;
  }
  
  private long contentLength() {
    if (contentLength == Long.MIN_VALUE) {
      contentLength = HttpHeaders.getContentLength(message, -1L);
    }
    return contentLength;
  }
  
  private LastHttpContent readTrailingHeaders(ByteBuf buffer) {
    headerSize = 0;
    AppendableCharSequence line = headerParser.parse(buffer);
    String lastHeader = null;
    if (line.length() > 0) {
      LastHttpContent trailer = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER, validateHeaders);
      do {
        char firstChar = line.charAt(0);
        if ((lastHeader != null) && ((firstChar == ' ') || (firstChar == '\t'))) {
          List<String> current = trailer.trailingHeaders().getAll(lastHeader);
          if (!current.isEmpty()) {
            int lastPos = current.size() - 1;
            String newString = (String)current.get(lastPos) + line.toString().trim();
            current.set(lastPos, newString);
          }
        }
        else
        {
          String[] header = splitHeader(line);
          String name = header[0];
          if ((!HttpHeaders.equalsIgnoreCase(name, "Content-Length")) && (!HttpHeaders.equalsIgnoreCase(name, "Transfer-Encoding")) && (!HttpHeaders.equalsIgnoreCase(name, "Trailer")))
          {

            trailer.trailingHeaders().add(name, header[1]);
          }
          lastHeader = name;
        }
        
        line = headerParser.parse(buffer);
      } while (line.length() > 0);
      
      return trailer;
    }
    
    return LastHttpContent.EMPTY_LAST_CONTENT; }
  
  protected abstract boolean isDecodingRequest();
  
  protected abstract HttpMessage createMessage(String[] paramArrayOfString) throws Exception;
  
  protected abstract HttpMessage createInvalidMessage();
  
  private static int getChunkSize(String hex) { hex = hex.trim();
    for (int i = 0; i < hex.length(); i++) {
      char c = hex.charAt(i);
      if ((c == ';') || (Character.isWhitespace(c)) || (Character.isISOControl(c))) {
        hex = hex.substring(0, i);
        break;
      }
    }
    
    return Integer.parseInt(hex, 16);
  }
  






  private static String[] splitInitialLine(AppendableCharSequence sb)
  {
    int aStart = findNonWhitespace(sb, 0);
    int aEnd = findWhitespace(sb, aStart);
    
    int bStart = findNonWhitespace(sb, aEnd);
    int bEnd = findWhitespace(sb, bStart);
    
    int cStart = findNonWhitespace(sb, bEnd);
    int cEnd = findEndOfString(sb);
    
    return new String[] { sb.substring(aStart, aEnd), sb.substring(bStart, bEnd), cStart < cEnd ? sb.substring(cStart, cEnd) : "" };
  }
  


  private static String[] splitHeader(AppendableCharSequence sb)
  {
    int length = sb.length();
    





    int nameStart = findNonWhitespace(sb, 0);
    for (int nameEnd = nameStart; nameEnd < length; nameEnd++) {
      char ch = sb.charAt(nameEnd);
      if ((ch == ':') || (Character.isWhitespace(ch))) {
        break;
      }
    }
    
    for (int colonEnd = nameEnd; colonEnd < length; colonEnd++) {
      if (sb.charAt(colonEnd) == ':') {
        colonEnd++;
        break;
      }
    }
    
    int valueStart = findNonWhitespace(sb, colonEnd);
    if (valueStart == length) {
      return new String[] { sb.substring(nameStart, nameEnd), "" };
    }
    



    int valueEnd = findEndOfString(sb);
    return new String[] { sb.substring(nameStart, nameEnd), sb.substring(valueStart, valueEnd) };
  }
  



  private static int findNonWhitespace(CharSequence sb, int offset)
  {
    for (int result = offset; result < sb.length(); result++) {
      if (!Character.isWhitespace(sb.charAt(result))) {
        break;
      }
    }
    return result;
  }
  
  private static int findWhitespace(CharSequence sb, int offset)
  {
    for (int result = offset; result < sb.length(); result++) {
      if (Character.isWhitespace(sb.charAt(result))) {
        break;
      }
    }
    return result;
  }
  
  private static int findEndOfString(CharSequence sb)
  {
    for (int result = sb.length(); result > 0; result--) {
      if (!Character.isWhitespace(sb.charAt(result - 1))) {
        break;
      }
    }
    return result;
  }
  
  private final class HeaderParser implements ByteBufProcessor {
    private final AppendableCharSequence seq;
    
    HeaderParser(AppendableCharSequence seq) {
      this.seq = seq;
    }
    
    public AppendableCharSequence parse(ByteBuf buffer) {
      seq.reset();
      headerSize = 0;
      int i = buffer.forEachByte(this);
      buffer.readerIndex(i + 1);
      return seq;
    }
    
    public boolean process(byte value) throws Exception
    {
      char nextByte = (char)value;
      HttpObjectDecoder.access$008(HttpObjectDecoder.this);
      if (nextByte == '\r') {
        return true;
      }
      if (nextByte == '\n') {
        return false;
      }
      

      if (headerSize >= maxHeaderSize)
      {



        throw new TooLongFrameException("HTTP header is larger than " + maxHeaderSize + " bytes.");
      }
      


      seq.append(nextByte);
      return true;
    }
  }
  
  private final class LineParser implements ByteBufProcessor {
    private final AppendableCharSequence seq;
    private int size;
    
    LineParser(AppendableCharSequence seq) {
      this.seq = seq;
    }
    
    public AppendableCharSequence parse(ByteBuf buffer) {
      seq.reset();
      size = 0;
      int i = buffer.forEachByte(this);
      buffer.readerIndex(i + 1);
      return seq;
    }
    
    public boolean process(byte value) throws Exception
    {
      char nextByte = (char)value;
      if (nextByte == '\r')
        return true;
      if (nextByte == '\n') {
        return false;
      }
      if (size >= maxInitialLineLength)
      {



        throw new TooLongFrameException("An HTTP line is larger than " + maxInitialLineLength + " bytes.");
      }
      

      size += 1;
      seq.append(nextByte);
      return true;
    }
  }
}
