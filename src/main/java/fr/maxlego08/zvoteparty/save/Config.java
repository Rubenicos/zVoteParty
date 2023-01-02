package fr.maxlego08.zvoteparty.save;

import fr.maxlego08.zvoteparty.api.storage.Storage;
import fr.maxlego08.zvoteparty.save.RedisConfiguration.RedisPoolConfiguration;
import fr.maxlego08.zvoteparty.zcore.utils.ProgressBar;
import fr.maxlego08.zvoteparty.zcore.utils.storage.Persist;
import fr.maxlego08.zvoteparty.zcore.utils.storage.Saveable;

public class Config implements Saveable {

	public static Storage storage = Storage.JSON;
	public static Storage redisSqlStorage = Storage.MYSQL;

	public static boolean enableDebug = false;
	public static boolean enableDebugTime = false;
	public static boolean enableAutoUpdate = true;
	public static boolean enableLogMessage = false;

	public static boolean enableVoteCommand = true;
	public static boolean enableVoteInventory = true;
	public static boolean enableVoteMessage = true;
	public static boolean enableInventoryPreRender = false;
	public static boolean enableOpenSyncInventory = false;

	public static boolean enableActionBarVoteAnnonce = true;
	public static boolean enableTchatVoteAnnonce = false;

	public static long joinGiveVoteMilliSecond = 500;
	public static long autoSaveSecond = 60 * 15;
	
	public static int redisServerAmount = 2;
	public static String redisChannel = "zvoteparty";
	public static RedisConfiguration redis = new RedisConfiguration("192.168.10.10", 6379, null, 0,
			new RedisPoolConfiguration(128, 128, 16));
	public static int maxSqlRetryAmoun = 5;
	
	public static ProgressBar progressBar = new ProgressBar(20, '|', "§a", "§8");

	/**
	 * static Singleton instance.
	 */
	private static volatile Config instance;

	/**
	 * Private constructor for singleton.
	 */
	private Config() {
	}

	/**
	 * Return a singleton instance of Config.
	 */
	public static Config getInstance() {
		// Double lock for thread safety.
		if (instance == null) {
			synchronized (Config.class) {
				if (instance == null) {
					instance = new Config();
				}
			}
		}
		return instance;
	}

	public void save(Persist persist) {
		persist.save(getInstance());
	}

	public void load(Persist persist) {
		persist.loadOrSaveDefault(getInstance(), Config.class);
	}

}
