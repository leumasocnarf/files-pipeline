package com.demo.processing.config

import com.demo.processing.events.FileProcessedEvent
import com.demo.processing.events.FileUploadedEvent
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
class KafkaConfig(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun fileProcessedTopic(): NewTopic =
        TopicBuilder.name("file.processed")
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun producerFactory(): ProducerFactory<String, FileProcessedEvent> {
        val props = kafkaProperties.buildProducerProperties().apply {
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer::class.java)
            put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.RETRIES_CONFIG, 3)
            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)
        }
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun kafkaTemplate(
        producerFactory: ProducerFactory<String, FileProcessedEvent>
    ): KafkaTemplate<String, FileProcessedEvent> =
        KafkaTemplate(producerFactory)

    @Bean
    fun consumerFactory(): ConsumerFactory<String, FileUploadedEvent> {
        val props = kafkaProperties.buildConsumerProperties().apply {
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer::class.java)
            put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.demo.*")
            put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, FileUploadedEvent::class.java.name)
            put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false)
        }
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, FileUploadedEvent>
    ): ConcurrentKafkaListenerContainerFactory<String, FileUploadedEvent> =
        ConcurrentKafkaListenerContainerFactory<String, FileUploadedEvent>().apply {
            setConsumerFactory(consumerFactory)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        }
}