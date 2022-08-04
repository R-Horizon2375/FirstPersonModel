package dev.tr7zw.firstperson;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.Vec3;

public class MinecraftWrapper {

	private final Minecraft client;
	private Vec3 offset; //Current offset used for rendering
	
	public MinecraftWrapper(Minecraft instance) {
		this.client = instance;
	}
	
	public String joinServerSession(String serverId) {
		try {
			client.getMinecraftSessionService().joinServer(
					client.getUser().getGameProfile(),
					client.getUser().getAccessToken(), serverId);
		} catch (AuthenticationUnavailableException var3) {
			return "Servers-Unavailable!";
		} catch (InvalidCredentialsException var4) {
			return "invalidSession";
		} catch (AuthenticationException var5) {
			return var5.getMessage();
		}
		return null; // Valid request
	}

	
	public GameProfile getGameprofile() {
		return client.getUser().getGameProfile();
	}

	
	public void showToastSuccess(String message, String submessage) {
		client.getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_BACKUP, new TextComponent(message), submessage == null ? null : new TextComponent(submessage)));
	}

	
	public void showToastFailure(String message, String submessage) {
		client.getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE, new TextComponent(message), submessage == null ? null : new TextComponent(submessage)));
	}
	
	public Object getPlayer() {
		return client.player;
	}

	
	public boolean applyThirdPerson(boolean thirdPerson) {
		if(client.player.isAutoSpinAttack())return false;
		if(client.player.isFallFlying())return false;
		if(client.player.getSwimAmount(1f) != 0 && !client.player.isVisuallySwimming())return false;
		if(!FirstPersonModelCore.enabled || thirdPerson)return false;
		return true;
	}

	
	public void refreshPlayerSettings() {
		client.options.broadcastOptions();
	}

	
	public void updatePositionOffset(Entity player, Vec3 defValue) {
		if(player == client.getCameraEntity() && client.player.isSleeping()) {
			offset = defValue;
			return;
		}
		double x,y,z = x = y = z = 0;
		AbstractClientPlayer abstractClientPlayerEntity_1;
		double realYaw;
		if(player == client.player && client.options.getCameraType() == CameraType.FIRST_PERSON && FirstPersonModelCore.isRenderingPlayer) {
			abstractClientPlayerEntity_1 = (AbstractClientPlayer) player;
			realYaw = Mth.rotLerp(client.getFrameTime(), abstractClientPlayerEntity_1.yRotO, abstractClientPlayerEntity_1.getYRot());
		}else {
			offset = defValue;
			return;
		}
		if (!abstractClientPlayerEntity_1.isLocalPlayer() || client.getCameraEntity() == abstractClientPlayerEntity_1) {
			float bodyOffset;
			if(client.player.isVisuallySwimming()) {
				abstractClientPlayerEntity_1.yBodyRot = abstractClientPlayerEntity_1.yHeadRot;
				if(abstractClientPlayerEntity_1.xRotO > 0) {
					bodyOffset = FirstPersonModelCore.swimUpBodyOffset;
				}else {
					bodyOffset = FirstPersonModelCore.swimDownBodyOffset;
				}
			}else if(abstractClientPlayerEntity_1.isShiftKeyDown()){
				bodyOffset = FirstPersonModelCore.sneakBodyOffset + (FirstPersonModelCore.config.sneakXOffset / 100f);
			}else if(abstractClientPlayerEntity_1.isPassenger()) {
				if(abstractClientPlayerEntity_1.getVehicle() instanceof Boat || abstractClientPlayerEntity_1.getVehicle() instanceof Minecart) {
					realYaw = Mth.rotLerp(client.getFrameTime(), abstractClientPlayerEntity_1.yBodyRotO, abstractClientPlayerEntity_1.yBodyRot);
				} else if(abstractClientPlayerEntity_1.getVehicle() instanceof LivingEntity){
					realYaw = Mth.rotLerp(client.getFrameTime(), ((LivingEntity)abstractClientPlayerEntity_1.getVehicle()).yBodyRotO, ((LivingEntity)abstractClientPlayerEntity_1.getVehicle()).yBodyRot);
				} else {
					realYaw = Mth.rotLerp(client.getFrameTime(), abstractClientPlayerEntity_1.getVehicle().yRotO, abstractClientPlayerEntity_1.getVehicle().getYRot());
				}
				bodyOffset = FirstPersonModelCore.inVehicleBodyOffset + (FirstPersonModelCore.config.sitXOffset / 100f);
			}else{
				bodyOffset = 0.25f + (FirstPersonModelCore.config.xOffset / 100f);
			}
			x += bodyOffset * Math.sin(Math.toRadians(realYaw));
			z -= bodyOffset * Math.cos(Math.toRadians(realYaw));
			if(client.player.isVisuallySwimming()) {
				if(abstractClientPlayerEntity_1.xRotO > 0  && abstractClientPlayerEntity_1.isUnderWater()) {
					y += 0.6f * Math.sin(Math.toRadians(abstractClientPlayerEntity_1.xRotO));
				}else {
					y += 0.01f * -Math.sin(Math.toRadians(abstractClientPlayerEntity_1.xRotO));
				}
			}

		}
		Vec3 vec = new Vec3(x, y, z);
		abstractClientPlayerEntity_1 = null;
		offset = vec;
	}

	
	public Vec3 getOffset() {
		return offset;
	}

	
	public boolean hasCustomSkin(Object player) {
		return !DefaultPlayerSkin.getDefaultSkin(((AbstractClientPlayer)player).getUUID()).equals(((AbstractClientPlayer)player).getSkinTextureLocation());
	}

	
	public Object getSkinTexture(Object player) {
		NativeImage skin = new NativeImage(Format.RGBA, 64, 64, true);
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		AbstractTexture abstractTexture = textureManager.getTexture(((AbstractClientPlayer)player).getSkinTextureLocation());
		GlStateManager._bindTexture(abstractTexture.getId());
		skin.downloadTexture(0, false);
		return skin;
	}

	
	public Object changeHue(Object ido, int hue) {
		ResourceLocation id = (ResourceLocation) ido;
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		ResourceLocation newId = new ResourceLocation(id.getNamespace(), id.getPath() + "_" + hue);
		if(textureManager.getTexture(newId) != null) {
			return newId;
		}
		AbstractTexture abstractTexture = textureManager.getTexture(id);
		if (abstractTexture == null) {
			return id;
		}
		GlStateManager._bindTexture(abstractTexture.getId());
		int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		NativeImage skin = new NativeImage(Format.RGBA, width, height, true);
		skin.downloadTexture(0, false);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (skin.getLuminanceOrAlpha(x, y) != 0) {
					int RGBA = skin.getPixelRGBA(x, y);
					int alpha = NativeImage.getA(RGBA);
					int R = (RGBA >> 16) & 0xff;
					int G = (RGBA >> 8) & 0xff;
					int B = (RGBA) & 0xff;
					float HSV[] = new float[3];
					Color.RGBtoHSB(R, G, B, HSV);
					Color fColor = Color.getHSBColor(HSV[0] + (hue/360f), HSV[1], HSV[2]);
					skin.setPixelRGBA(x, y, NativeImage.combine(alpha, fColor.getRed(), fColor.getGreen(), fColor.getBlue()));
				}
			}
		}
		textureManager.register(newId, new DynamicTexture(skin));
		return newId;
	}

	
	public Object getIdentifier(String namespace, String id) {
		return new ResourceLocation(namespace, id);
	}

}
