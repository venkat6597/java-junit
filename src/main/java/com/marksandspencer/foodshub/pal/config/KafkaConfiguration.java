package com.marksandspencer.foodshub.pal.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "confluent.kafka", value = "enabled", havingValue = "true")
@ComponentScan(basePackages = {"com.marksandspencer.foodshub.pal.event"})
public class KafkaConfiguration {

    @Value("${confluent.kafka.servers}")
    String bootstrapServers;

    @Value("${confluent.kafka.message.maxretries}")
    String maxRetries;

    @Value("${confluent.kafka.properties.security.protocol}")
    String securityProtocal;

    @Value("${confluent.kafka.properties.sasl.jaas.config}")
    String saslJassConfig;

    @Value("${confluent.kafka.properties.sasl.mechanism}")
    String saslMechanism;

    @Value("${confluent.kafka.message.clientid}")
    String clientId;

    @Bean
    public ProducerFactory<String, Object> palMessageProducer(){
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(CommonClientConfigs.RETRIES_CONFIG, maxRetries);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocal);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put("sasl.jaas.config", saslJassConfig);
        props.put("sasl.mechanism", saslMechanism);
        props.put("ssl.endpoint.identification.algorithm", "https");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(palMessageProducer());
    }
}
