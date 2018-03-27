# OG-Kafka

This repo builds a kafka docker container based on confluent/kafka-cp:4.0 and embeds a statsd metrics forwarder suitable for deploying kafka in kubernetes at opengov.  What that means is the kafka container produced by this project will forward all of the kafka server metrics (Yammer metrics) to a configured statsd host.  The intent is for the metrics to be forwarded to a dogstatsd service running in kubernetes, which will make all of the kafka server metrics available in datadog for dashboards and alerting.

## The Project
This is a gradle project forked from from airbnb's [kafka-statsd-metrics2 repo](https://github.com/airbnb/kafka-statsd-metrics2/). That project was built against 
kafka-0.9, so this fork updates the dependencies to kafka 1.0.0, since that is the version of kafka that confluent/kafka-cp provides.  The metrics reporter tags all of the 
Kafka 1.0.0 specific server metrics and forwards them to a configured statsd host.

This project also defines a couple additional tasks to create a docker container based on confluent/kafka-cp:4.0.  The buildImage task will embed the output of the shadow jar 
task (the metrics reporter) into the container java library path for kafka, as well as defines environment variables to configure the metrics reporter.

see [README-airbnb.md] for details on the statsd metrics reporter framework.  

## Building

Build just like a normal gradle java project:

```./gradlew build```

To build the container (and push to the opengov repository)

```./gradlew buildImage```

## Usage
This container is used specifically in the [og-kafka helm chart](https://github.com/OpenGov/infrastructure/tree/master/helm_charts/kafka) 

Using the image directly presupposes a zookeeper installation and a number of other configurations.  The helm chart above is the recommended way to deploy 
the image to either kubernetes or minikube.

## Container
The docker task embeds the statsd metric reporter into the kafka lib folders and injects configuration via environment variables, specificially:

envar | meaning | value 
--- | --- | ---
"KAFKA_METRIC_REPORTERS" | the class used to report kafka yammer metrics | "com.airbnb.kafka.kafka09.StatsdMetricsReporter" 
"KAFKA_EXTERNAL_KAFKA_STATSD_TAG_ENABLED" | flag to enable/disable datadog style metrics tagging | "true"
"KAFKA_EXTERNAL_KAFKA_STATSD_REPORTER_ENABLED" | flag to enable/disable statsd metrics forwarding | "true"
"KAFKA_EXTERNAL_KAFKA_STATSD_HOST" | host for the statd daemon. This should be overridden in k8s, e.g. base-datadog.management | "localhost"
"KAFKA_EXTERNAL_KAFKA_STATSD_PORT" | port for the statsd daemon | "8125"

## Changelog

### 0.1
- initial build of og-kafka

