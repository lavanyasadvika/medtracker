FROM prom/prometheus:v2.55.1

COPY monitoring/prometheus/prometheus.yml /etc/prometheus/prometheus.yml
