package fr.maxlego08.zvoteparty.api.sound;

import com.cryptomorin.xseries.XSound;
import org.bukkit.entity.Entity;

public interface SoundOption {

	/**
	 * 
	 * @return sound
	 */
	public XSound getSound();
	
	/**
	 * 
	 * @return pitch
	 */
	public float getPitch();
	
	/**
	 * 
	 * @return volume
	 */
	public float getVolume();
	
	/**
	 * 
	 * @param entity
	 */
	void play(Entity entity);
	
}
