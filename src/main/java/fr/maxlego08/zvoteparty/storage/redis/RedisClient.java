package fr.maxlego08.zvoteparty.storage.redis;

import fr.maxlego08.zvoteparty.save.Config;
import fr.maxlego08.zvoteparty.save.RedisConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisClient {
	
	private final JedisPool pool;

	public RedisClient() {
		RedisConfiguration config = Config.redis;

		this.pool = config.getPassword() != null
				? new JedisPool(buildPoolConfig(config), config.getHost(), config.getPort(), Protocol.DEFAULT_TIMEOUT, config.getPassword())
				: new JedisPool(buildPoolConfig(config), config.getHost(), config.getPort());

	}

	public JedisPool getPool() {
		return pool;
	}

	public Jedis getResource() {
		final Jedis jedis = this.pool.getResource();

		final int index = Config.redis.getDatabaseIndex();
		if (index != 0) {
			jedis.select(index);
		}

		return jedis;
	}

	private JedisPoolConfig buildPoolConfig(RedisConfiguration config) {
		RedisConfiguration.RedisPoolConfiguration poolConfig = config.getPoolConfig();
		JedisPoolConfig poolConfigBuilder = new JedisPoolConfig();

		poolConfigBuilder.setMaxTotal(poolConfig.getMaxTotal());
		poolConfigBuilder.setMaxIdle(poolConfig.getMaxIdle());
		poolConfigBuilder.setMinIdle(poolConfig.getMinIdle());

		return poolConfigBuilder;
	}
}
