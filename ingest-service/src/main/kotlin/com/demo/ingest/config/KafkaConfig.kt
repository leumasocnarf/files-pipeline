package com.demo.ingest.config

import com.demo.ingest.events.FileUploadedEvent
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
class KafkaConfig(private val kafkaProperties: KafkaProperties) {

    @Bean
    fun fileUploadedTopic(): NewTopic =
        TopicBuilder.name("file.uploaded")
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun producerFactory(): ProducerFactory<String, FileUploadedEvent> {
        val props = kafkaProperties.buildProducerProperties().apply {
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer::class.java)
            put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false)
        }
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun kafkaTemplate(
        producerFactory: ProducerFactory<String, FileUploadedEvent>
    ): KafkaTemplate<String, FileUploadedEvent> =
        KafkaTemplate(producerFactory)
}