package com.demo.report.config

import com.demo.report.events.FileProcessedEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer

@Configuration
class KafkaConfig(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun consumerFactory(): ConsumerFactory<String, FileProcessedEvent> {
        val props = kafkaProperties.buildConsumerProperties().apply {
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer::class.java)
            put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.demo.*")
            put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, FileProcessedEvent::class.java.name)
            put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false)
        }
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, FileProcessedEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, FileProcessedEvent> {
        return ConcurrentKafkaListenerContainerFactory<String, FileProcessedEvent>().apply {
            setConsumerFactory(consumerFactory)
        }
    }
}