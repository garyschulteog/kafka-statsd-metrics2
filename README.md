# OG-Kafka

This repo builds a kafka docker container based on confluent/kafka-cp:4.0 and embeds a statsd metrics forwarder, originally forked from airbnb's [kafka-statsd-metrics2 repo](https://github.com/airbnb/kafka-statsd-metrics2/)

## Container
This project is built against kafka-1.0.0 to match with the version in the confluent/kafka-cp container, and includes a docker container generation task.
The docker task embeds the statsd metric reporter into the kafka lib folders and injects configuration via environment variables, specificially:

envar | value | meaning
--- | --- | ---
"KAFKA_METRIC_REPORTERS" | "com.airbnb.kafka.kafka09.StatsdMetricsReporter" | the class used to report kafka yammer metrics
"KAFKA_EXTERNAL_KAFKA_STATSD_TAG_ENABLED" | "true" | flag to enable/disable datadog style metrics tagging
"KAFKA_EXTERNAL_KAFKA_STATSD_REPORTER_ENABLED" | "true" | flag to enable/disable statsd metrics forwarding
"KAFKA_EXTERNAL_KAFKA_STATSD_HOST" | "localhost" | host for the statd daemon. This should be overridden in k8s, e.g. base-datadog.management
"KAFKA_EXTERNAL_KAFKA_STATSD_PORT" | "8125" | port for the statsd daemon

This container is used specifically in the [og-kafka helm chart](https://github.com/OpenGov/infrastructure/tree/master/helm_charts/kafka) 

see [README-airbnb.md] for details on the statsd metrics reporter framework.  

## Building

Build just like a normal gradle java project:
```./gradlew build```

To build the container (and push to the opengov repository)
```./gradlew buildImage```


## Releases

### 0.1
- initial build of og-kafka

