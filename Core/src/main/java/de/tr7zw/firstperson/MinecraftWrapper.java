package de.tr7zw.firstperson;

import com.mojang.authlib.GameProfile;

public interface MinecraftWrapper {

	public String joinServerSession(String serverId);
	public GameProfile getGameprofile();
	public void showToastSuccess(String message, String submessage);
	public void showToastFailure(String message, String submessage);
	public void sendNoLayerClientSettings();
	public Object getPlayer();
	public boolean applyThirdPerson(boolean thirdPerson);
	public void refreshPlayerSettings();
	/**
	 * run it, if the renderer is rendering our scene, to set the head hidden.
	 */
 	public void isThirdPersonTrigger(Object matrices);
}
