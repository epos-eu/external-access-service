package org.epos.configuration;

import java.time.Duration;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.ClientOptions.DisconnectedBehavior;

@Configuration
@EnableRedisRepositories(value = "org.epos.core.beans.repositories")
public class RedisConfiguration {
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
    	RedisProperties properties = redisProperties();
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();

		configuration.setHostName(properties.getHost());
		configuration.setPort(properties.getPort());

		ClientOptions clientOptions = ClientOptions.builder()  
				.timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(60)))
				.autoReconnect(true)
				.disconnectedBehavior(DisconnectedBehavior.DEFAULT)
				.build();  

		LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()  
				.commandTimeout(Duration.ofSeconds(60))   
				.readFrom(ReadFrom.ANY) // Preferentially read data from the replicas.
				.clientOptions(clientOptions)  
				.build();  
		LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration, clientConfig);  
		return factory;  
    }

    @Bean
    public RedisTemplate<byte[], byte[]> redisTemplate() {
        RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();

        template.setConnectionFactory(redisConnectionFactory());

        return template;
    }

    @Bean
    @Primary
    public RedisProperties redisProperties() {
        return new RedisProperties();
    }
}