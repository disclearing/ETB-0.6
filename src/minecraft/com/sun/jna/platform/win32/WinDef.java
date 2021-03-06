package com.sun.jna.platform.win32;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import java.awt.Rectangle;





















public abstract interface WinDef
  extends StdCallLibrary
{
  public static final int MAX_PATH = 260;
  
  public static class WORD
    extends IntegerType
  {
    public WORD()
    {
      this(0L);
    }
    
    public WORD(long value) {
      super(value);
    }
  }
  
  public static class DWORD
    extends IntegerType
  {
    public DWORD()
    {
      this(0L);
    }
    
    public DWORD(long value) {
      super(value, true);
    }
    




    public WinDef.WORD getLow()
    {
      return new WinDef.WORD(longValue() & 0xFF);
    }
    




    public WinDef.WORD getHigh()
    {
      return new WinDef.WORD(longValue() >> 16 & 0xFF);
    }
  }
  
  public static class LONG
    extends IntegerType
  {
    public LONG()
    {
      this(0L);
    }
    
    public LONG(long value) {
      super(value);
    }
  }
  

  public static class HDC
    extends WinNT.HANDLE
  {
    public HDC() {}
    

    public HDC(Pointer p)
    {
      super();
    }
  }
  

  public static class HICON
    extends WinNT.HANDLE
  {
    public HICON() {}
    

    public HICON(Pointer p)
    {
      super();
    }
  }
  

  public static class HCURSOR
    extends WinDef.HICON
  {
    public HCURSOR() {}
    

    public HCURSOR(Pointer p)
    {
      super();
    }
  }
  

  public static class HMENU
    extends WinNT.HANDLE
  {
    public HMENU() {}
    

    public HMENU(Pointer p)
    {
      super();
    }
  }
  

  public static class HPEN
    extends WinNT.HANDLE
  {
    public HPEN() {}
    

    public HPEN(Pointer p)
    {
      super();
    }
  }
  

  public static class HRSRC
    extends WinNT.HANDLE
  {
    public HRSRC() {}
    

    public HRSRC(Pointer p)
    {
      super();
    }
  }
  

  public static class HPALETTE
    extends WinNT.HANDLE
  {
    public HPALETTE() {}
    

    public HPALETTE(Pointer p)
    {
      super();
    }
  }
  

  public static class HBITMAP
    extends WinNT.HANDLE
  {
    public HBITMAP() {}
    

    public HBITMAP(Pointer p)
    {
      super();
    }
  }
  

  public static class HRGN
    extends WinNT.HANDLE
  {
    public HRGN() {}
    

    public HRGN(Pointer p)
    {
      super();
    }
  }
  

  public static class HWND
    extends WinNT.HANDLE
  {
    public HWND() {}
    

    public HWND(Pointer p)
    {
      super();
    }
  }
  

  public static class HINSTANCE
    extends WinNT.HANDLE
  {
    public HINSTANCE() {}
  }
  

  public static class HMODULE
    extends WinDef.HINSTANCE
  {
    public HMODULE() {}
  }
  

  public static class HFONT
    extends WinNT.HANDLE
  {
    public HFONT() {}
    

    public HFONT(Pointer p)
    {
      super();
    }
  }
  
  public static class LPARAM
    extends BaseTSD.LONG_PTR
  {
    public LPARAM()
    {
      this(0L);
    }
    
    public LPARAM(long value) {
      super();
    }
  }
  
  public static class LRESULT
    extends BaseTSD.LONG_PTR
  {
    public LRESULT()
    {
      this(0L);
    }
    
    public LRESULT(long value) {
      super();
    }
  }
  
  public static class UINT_PTR
    extends IntegerType
  {
    public UINT_PTR()
    {
      super();
    }
    
    public UINT_PTR(long value) {
      super(value);
    }
    
    public Pointer toPointer() {
      return Pointer.createConstant(longValue());
    }
  }
  
  public static class WPARAM
    extends WinDef.UINT_PTR
  {
    public WPARAM()
    {
      this(0L);
    }
    

    public WPARAM(long value) { super(); }
  }
  
  public static class RECT extends Structure {
    public int left;
    public int top;
    public int right;
    public int bottom;
    
    public RECT() {}
    
    public Rectangle toRectangle() { return new Rectangle(left, top, right - left, bottom - top); }
    
    public String toString()
    {
      return "[(" + left + "," + top + ")(" + right + "," + bottom + ")]";
    }
  }
  
  public static class ULONGLONG
    extends IntegerType
  {
    public ULONGLONG()
    {
      this(0L);
    }
    
    public ULONGLONG(long value) {
      super(value);
    }
  }
  
  public static class DWORDLONG
    extends IntegerType
  {
    public DWORDLONG()
    {
      this(0L);
    }
    
    public DWORDLONG(long value) {
      super(value);
    }
  }
}
