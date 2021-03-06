package net.minecraft.network.login;

import net.minecraft.network.INetHandler;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.login.server.S03PacketEnableCompression;

public abstract interface INetHandlerLoginClient
  extends INetHandler
{
  public abstract void handleEncryptionRequest(S01PacketEncryptionRequest paramS01PacketEncryptionRequest);
  
  public abstract void handleLoginSuccess(S02PacketLoginSuccess paramS02PacketLoginSuccess);
  
  public abstract void handleDisconnect(S00PacketDisconnect paramS00PacketDisconnect);
  
  public abstract void func_180464_a(S03PacketEnableCompression paramS03PacketEnableCompression);
}
