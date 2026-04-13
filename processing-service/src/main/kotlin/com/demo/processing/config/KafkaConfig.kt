package com.demo.processing.config

import com.demo.processing.events.FileProcessedEvent
import com.demo.processing.events.FileUploadedEvent
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
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
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties

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
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer::class.java)
            put("schema.registry.url", "http://schema-registry:8085")
            put("auto.register.schemas", true)
            put("json.fail.invalid.schema", true)
            put("json.schema.spec.version", "draft_2020_12")
            put("use.latest.version", true)
            put("latest.compatibility.strict", false)
            put("json.write.dates.iso8601", true)
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
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonSchemaDeserializer::class.java)
            put("schema.registry.url", "http://schema-registry:8085")
            put("use.latest.version", true)
            put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, FileUploadedEvent::class.java.name)
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