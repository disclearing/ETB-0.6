package net.minecraft.network.login;

import net.minecraft.network.INetHandler;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;

public abstract interface INetHandlerLoginServer
  extends INetHandler
{
  public abstract void processLoginStart(C00PacketLoginStart paramC00PacketLoginStart);
  
  public abstract void processEncryptionResponse(C01PacketEncryptionResponse paramC01PacketEncryptionResponse);
}
