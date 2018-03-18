# add statsd kafka logging to confluent kafka docker container

FROM confluentinc/cp-kafka:4.0.0

ENV KAFKA_METRIC_REPORTERS=com.airbnb.kafka.kafka09.StatsdMetricsReporter
ENV KAFKA_EXTERNAL_KAFKA_STATSD_TAG_ENABLED=true
#ENV KAFKA_EXTERNAL_KAFKA_STATSD_METRICS_PREFIX=kafka_
ENV KAFKA_EXTERNAL_KAFKA_STATSD_REPORTER_ENABLED=true
ENV KAFKA_EXTERNAL_KAFKA_STATSD_HOST=localhost
ENV KAFKA_EXTERNAL_KAFKA_STATSD_PORT=8125
ADD build/libs/kafka-statsd-metrics2-0.5.2-all.jar /usr/share/java/kafka/




