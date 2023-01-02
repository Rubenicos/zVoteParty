package fr.maxlego08.zvoteparty.button.buttons;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import fr.maxlego08.zvoteparty.api.button.Button;
import fr.maxlego08.zvoteparty.api.button.buttons.PermissibleButton;
import fr.maxlego08.zvoteparty.api.enums.ButtonType;
import fr.maxlego08.zvoteparty.api.sound.SoundOption;
import fr.maxlego08.zvoteparty.button.ZButton;

public class ZPermissibleButton extends ZButton implements PermissibleButton {

	private final String permission;
	private final String message;
	private final Button elseButton;
	private final boolean glowIfCheck;

	/**
	 * @param type
	 * @param itemStack
	 * @param slot
	 * @param permission
	 * @param elseButton
	 */
	public ZPermissibleButton(ButtonType type, ItemStack itemStack, int slot, String permission, String message,
			Button elseButton, boolean isPermanent, boolean glowIfCheck, SoundOption sound) {
		super(type, itemStack, slot, isPermanent, sound);
		this.permission = permission;
		this.elseButton = elseButton;
		this.message = color(message);
		this.glowIfCheck = glowIfCheck;
	}

	/**
	 * @param type
	 * @param itemStack
	 * @param slot
	 * @param permission
	 * @param elseButton
	 */
	public ZPermissibleButton(ButtonType type, ItemStack itemStack, int slot, boolean isPermanent, SoundOption sound) {
		super(type, itemStack, slot, isPermanent, sound);
		this.permission = null;
		this.elseButton = null;
		this.message = null;
		this.glowIfCheck = false;
	}

	@Override
	public Button getElseButton() {
		return elseButton;
	}

	@Override
	public String getPermission() {
		return permission;
	}

	@Override
	public boolean hasPermission() {
		return permission != null;
	}

	@Override
	public boolean hasElseButton() {
		return elseButton != null;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public boolean hasMessage() {
		return message != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ZPermissibleButton [permission=" + permission + ", message=" + message + ", elseButton=" + elseButton
				+ "] => " + super.toString();
	}

	@Override
	public boolean checkPermission(Player player) {
		return this.getPermission() == null || player.hasPermission(this.getPermission());
	}

	@Override
	public boolean needToGlow() {
		return glowIfCheck;
	}

	@Override
	public ItemStack getCustomItemStack(Player player) {
		ItemStack itemStack = super.getCustomItemStack(player);
		if (checkPermission(player) && needToGlow())
			glow(itemStack);
		return itemStack;
	}

}
