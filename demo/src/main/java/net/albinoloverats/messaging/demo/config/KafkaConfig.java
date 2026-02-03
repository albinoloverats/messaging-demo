package net.albinoloverats.messaging.demo.config;

import lombok.val;
import net.albinoloverats.messaging.demo.messages.ExampleEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;

@EnableKafka
@Configuration
@Profile("kafka")
public class KafkaConfig
{
	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapAddress;

	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;

	/*
	 * Consumer
	 */

	public <T> ConsumerFactory<String, T> consumerFactory(Class<T> targetType, String trustedPackages)
	{
		val props = new HashMap<String, Object>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);
		props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
		props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
		return new DefaultKafkaConsumerFactory<>(props);
	}

	public <T> ConcurrentKafkaListenerContainerFactory<String, T> kafkaListenerContainerFactory(Class<T> targetType, String trustedPackages)
	{
		val factory = new ConcurrentKafkaListenerContainerFactory<String, T>();
		factory.setConsumerFactory(consumerFactory(targetType, trustedPackages));
		return factory;
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, ExampleEvent> exampleEventKafkaListenerContainerFactory()
	{
		return kafkaListenerContainerFactory(ExampleEvent.class, ExampleEvent.class.getPackageName());
	}

	/*
	 * Producer
	 */

	@Bean
	public <T> ProducerFactory<String, T> producerFactory()
	{
		val configProps = new HashMap<String, Object>();
		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
		configProps.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
		return new DefaultKafkaProducerFactory<>(configProps);
	}

	@Bean
	public <T> KafkaTemplate<String, T> kafkaTemplate()
	{
		return new KafkaTemplate<>(producerFactory());
	}
}

