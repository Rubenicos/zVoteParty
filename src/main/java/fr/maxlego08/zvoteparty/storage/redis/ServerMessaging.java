package fr.maxlego08.zvoteparty.storage.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import fr.maxlego08.zvoteparty.ZVotePartyPlugin;
import fr.maxlego08.zvoteparty.api.storage.RedisSubChannel;
import fr.maxlego08.zvoteparty.save.Config;
import fr.maxlego08.zvoteparty.storage.storages.RedisStorage;
import fr.maxlego08.zvoteparty.zcore.logger.Logger;
import fr.maxlego08.zvoteparty.zcore.logger.Logger.LogType;
import org.intellij.lang.annotations.Language;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class ServerMessaging extends JedisPubSub {

	private static final @Language("RegExp") String SEPARATOR = ";;";

	private final ZVotePartyPlugin plugin;
	private final RedisStorage storage;
	private final RedisClient client;

	private final Thread threadMessaging1;
	boolean enabled = true;

	private final List<UUID> sendingUUID = new ArrayList<>();
	private final Map<UUID, RedisVoteResponse> voteResponses = new HashMap<>();

	/**
	 * @param plugin
	 * @param storage
	 * @param client
	 */
	public ServerMessaging(ZVotePartyPlugin plugin, RedisStorage storage, RedisClient client) {
		super();
		this.plugin = plugin;
		this.storage = storage;
		this.client = client;

		this.threadMessaging1 = new Thread(() -> {
			boolean reconnected = false;
			while (enabled && !Thread.interrupted() && this.client.getPool() != null && !this.client.getPool().isClosed()) {
				try (Jedis jedis = this.client.getResource()) {
					if (reconnected) {
						Logger.info("Redis connection is alive again", Logger.LogType.SUCCESS);
					}
					// Lock the thread
					jedis.subscribe(this, Config.redisChannel);
				} catch (Throwable t) {
					// Thread was unlocked after error
					if (enabled) {
						if (reconnected) {
							Logger.info("Redis connection dropped, automatic reconnection in 8 seconds...", Logger.LogType.WARNING);
							t.printStackTrace();
						}
						try {
							this.unsubscribe(Config.redisChannel);
						} catch (Throwable ignored) { }

						// Make an instant subscribe if occurs any error on initialization
						if (!reconnected) {
							reconnected = true;
						} else {
							try {
								Thread.sleep(8000);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					} else {
						return;
					}
				}
			}
		});
		this.threadMessaging1.start();
	}

	@Override
	public void onMessage(String channel, String message) {

		if (!this.plugin.isEnabled()) {
			return;
		}

		try {
			if (channel.equals(Config.redisChannel)) {

				final String[] values = message.split(SEPARATOR);
				if (values.length < 2) {
					return;
				}

				final UUID uuid = UUID.fromString(values[1]);

				// Allows to verify that the server sending the information does
				// not receive it.
				if (this.sendingUUID.contains(uuid)) {
					this.sendingUUID.remove(uuid);
					return;
				}

				switch (RedisSubChannel.byName(values[0])) {
					case ADD_VOTEPARTY:
						this.storage.addSecretVoteCount(1);
						break;
					case HANDLE_VOTEPARTY:
						this.storage.setSecretVoteCount(0);
						this.plugin.getManager().secretStart();
						break;
					case ADD_VOTE:
						if (values.length < 4) {
							return;
						}
						String username = values[2];
						String serviceName = values[3];
						if (!this.plugin.getManager().secretVote(username, serviceName)) {
							this.handleVoteResponseError(uuid, username, serviceName);
						} else {
							this.handleVoteResponse(uuid, true, null);
						}
						break;
					case VOTE_RESPONSE:
						if (values.length < 5) {
							return;
						}
						UUID messageId = UUID.fromString(values[2]);
						boolean isSuccess = Boolean.parseBoolean(values[3]);
						String userId = values[4];
						this.processResponse(messageId, isSuccess, userId);
						break;
					default:
						break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		Logger.info("zVoteParty subscribed to the redis channel \"" + channel + '"');
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		Logger.info("zVoteParty unsubscribed to the redis channel \"" + channel + '"');
	}

	/**
	 * Allows you to send a message
	 * 
	 * @param channel
	 * @param message
	 */
	private UUID sendMessage(RedisSubChannel channel, String message) {
		final UUID uuid = UUID.randomUUID();
		this.sendingUUID.add(uuid);

		final String jMessage;
		if (message != null) {
			jMessage = channel.name() + SEPARATOR + uuid + SEPARATOR + message;
		} else {
			jMessage = channel.name() + SEPARATOR + uuid;
		}

		if (Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> sendMessage(jMessage));
		} else {
			sendMessage(jMessage);
		}
		return uuid;
	}

	private void sendMessage(String message) {
		try (Jedis jedis = this.client.getResource()) {
			jedis.publish(Config.redisChannel, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Allows to stop the thread
	 */
	public void stop() {
		enabled = false;
		if (client.getPool() != null && client.getResource().getClient() != null) {
			this.unsubscribe(Config.redisChannel);
			if (!client.getPool().isClosed()) {
				client.getPool().close();
			}
		}
		if (threadMessaging1 != null) {
			threadMessaging1.interrupt();
		}
	}

	/**
	 * Allows you to send a message
	 * 
	 * @param channel
	 */
	private UUID sendMessage(RedisSubChannel channel) {
		return this.sendMessage(channel, null);
	}

	/**
	 * Allows you to add a vote to the voteparty
	 */
	public void sendAddVoteCount() {
		this.sendMessage(RedisSubChannel.ADD_VOTEPARTY);
	}

	/**
	 * Allows you to send the information to start the voting party
	 */
	public void sendHandleVoteParty() {
		this.sendMessage(RedisSubChannel.HANDLE_VOTEPARTY);
	}

	/**
	 * Allows you to send the voting action
	 * 
	 * @param username
	 * @param serviceName
	 * @param uuid
	 */
	public void sendVoteAction(String username, String serviceName, UUID uuid) {

		String message = username + ";;" + serviceName;
		UUID messageId = this.sendMessage(RedisSubChannel.ADD_VOTE, message);

		// Allows to give the reward if the player is not connected
		RedisVoteResponse redisVoteResponse = new RedisVoteResponse(messageId, username, serviceName, 1, uuid);
		this.voteResponses.put(messageId, redisVoteResponse);

	}

	@SuppressWarnings("deprecation")
	/**
	 * Allows you to reply to the server that sent the voting request to say
	 * that the player is not allowed to vote
	 * 
	 * @param uuid
	 * @param username
	 * @param serviceName
	 */
	private void handleVoteResponseError(UUID uuid, String username, String serviceName) {

		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
		this.handleVoteResponse(uuid, false, offlinePlayer != null ? offlinePlayer.getUniqueId().toString() : null);

	}

	/**
	 * We will return the answer with all the information
	 * 
	 * @param uuid
	 * @param isSuccess
	 * @param message
	 */
	private void handleVoteResponse(UUID uuid, boolean isSuccess, String message) {

		String jMessage = uuid.toString() + this.SEPARATOR + String.valueOf(isSuccess) + this.SEPARATOR + message;
		this.sendMessage(RedisSubChannel.VOTE_RESPONSE, jMessage);

	}

	/**
	 * Allows you to perform an action when receiving the voting confirmation
	 * 
	 * @param messageId
	 *            Identifier of the message that sent the request to vote
	 * @param isSuccess
	 *            Allows to know if the vote is successful
	 * @param userId
	 *            Player's UUID
	 */
	private void processResponse(UUID messageId, boolean isSuccess, String userId) {

		RedisVoteResponse redisVoteResponse = this.voteResponses.getOrDefault(messageId, null);

		// If the redis vote response is null, then the messageID is incorrect
		// or the value is delete
		if (redisVoteResponse == null) {
			return;
		}

		// If the answer is a success, then we delete the value
		if (isSuccess) {
			this.voteResponses.remove(messageId);
		} else {

			// Otherwise we check that the number of responses corresponds to
			// the number of servers indicated in the configuration file
			// We also add the UUID of the player if it is present

			redisVoteResponse.addResponse(userId);

			if (redisVoteResponse.getResponseCount() >= Config.redisServerAmount) {

				// We will check if the UUID of the player exists, if yes then
				// we will give a reward so that the player can recover it when
				// he will connect

				// If the player cannot be found, then nothing can be done and
				// the vote will be lost

				if (redisVoteResponse.getUserId() != null) {

					this.plugin.getManager().voteOffline(redisVoteResponse.getUserId(),
							redisVoteResponse.getServiceName());

				} else {

					Logger.info("Impossible to find the player " + redisVoteResponse.getUsername(), LogType.WARNING);

				}

			}

		}

	}

}
