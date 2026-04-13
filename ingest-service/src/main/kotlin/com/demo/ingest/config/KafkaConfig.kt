package com.demo.ingest.config

import com.demo.ingest.events.FileUploadedEvent
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer
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
        producerFactory: ProducerFactory<String, FileUploadedEvent>
    ): KafkaTemplate<String, FileUploadedEvent> =
        KafkaTemplate(producerFactory)
}