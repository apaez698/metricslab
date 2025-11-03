# Laboratorio de Observabilidad con OpenTelemetry, Jaeger, Prometheus y Grafana

## Descripción general

Este laboratorio tiene como objetivo comprender y montar, en entorno
local, un **pipeline completo de observabilidad distribuida** para una
aplicación **Spring Boot (Kotlin)** instrumentada con **Micrometer** y
el **OpenTelemetry Java Agent**.

El entorno utiliza **Podman Compose** para orquestar los componentes de
recolección y visualización:

- **OpenTelemetry Collector** → receptor y exportador de
  trazas/métricas.
- **Jaeger** → visualización de trazas distribuidas.
- **Prometheus** → recolección de métricas.
- **Grafana** → visualización de métricas.
- **Micrometer + OTel Agent** → generación de métricas y trazas desde
  la app.

------------------------------------------------------------------------

## Arquitectura General

    ┌───────────────────┐
    │   Spring Boot App  │
    │ (Micrometer + OTel │
    │       Agent)       │
    └────────┬───────────┘
             │ OTLP (gRPC,4317)
             ▼
    ┌───────────────────┐
    │ OTel Collector     │
    │ (receivers, batch, │
    │ exporters: OTLP→Jaeger)│
    └────────┬───────────┘
             │ OTLP interno
             ▼
    ┌───────────────────┐
    │      Jaeger       │
    │ (UI:16686)        │
    └───────────────────┘

    Prometheus ← /actuator/prometheus + :8888/metrics → Grafana

------------------------------------------------------------------------

## Componentes del laboratorio

### 1. Aplicación Spring Boot (Kotlin)

- Expone `/actuator/prometheus` para métricas de Micrometer.
- El **OTel Java Agent** instrumenta automáticamente peticiones HTTP,
  JDBC y controladores.
- Exporta spans vía **OTLP/gRPC** a `127.0.0.1:4317`.

#### VM Options (IntelliJ)

``` bash
-javaagent:/Users/sebastian.paez/Documents/Labs/micrometer-otel-lab/otel-javaagent.jar
-Dotel.traces.exporter=otlp
-Dotel.exporter.otlp.traces.endpoint=http://127.0.0.1:4317
-Dotel.exporter.otlp.traces.protocol=grpc
-Dotel.traces.sampler=always_on
-Dotel.resource.attributes=service.name=demo-payments,service.version=0.1.0,deployment.environment=local
-Dotel.javaagent.debug=true
```

### 2. OpenTelemetry Collector

**Imagen:** `otel/opentelemetry-collector:0.105.0`

**Función:** recibe spans vía OTLP, los agrupa (batch processor) y los
exporta a Jaeger por OTLP gRPC.

#### Configuración (`otel-collector-config.yaml`)

``` yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 1s
    send_batch_size: 512

exporters:
  otlp:
    endpoint: jaeger:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp]
  telemetry:
    metrics:
      address: ":8888"
```

### 3. Jaeger

**Imagen:** `jaegertracing/all-in-one:1.57`

**Configuración mínima**

``` yaml
environment:
  - COLLECTOR_OTLP_ENABLED=true
ports:
  - "16686:16686"
```

### 4. Prometheus

**Imagen:** `prom/prometheus:v2.54.1`

#### Configuración (`prometheus.yml`)

``` yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: "kotlin-app"
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["host.containers.internal:8080"]

  - job_name: "otel-collector"
    static_configs:
      - targets: ["host.containers.internal:8888"]
```

### 5. Grafana

**Imagen:** `grafana/grafana:11.2.2`

**Configuración:** - Data source: Prometheus
(`http://prometheus:9090`) - Dashboards para latencias, throughput y
spans enviados.

------------------------------------------------------------------------

## Conceptos técnicos utilizados

  -----------------------------------------------------------------------
Concepto Descripción
  ------------------------------- ---------------------------------------
**Telemetry**                   Conjunto de métricas, logs y trazas
recolectadas para observabilidad.

**OpenTelemetry (OTel)**        Framework estándar para generación,
procesamiento y exportación de
telemetry data.

**OTLP**                        OpenTelemetry Protocol: estándar
binario para enviar spans y métricas
(HTTP o gRPC).

**Span**                        Unidad mínima de una traza: representa
una operación o request.

**Trace**                       Conjunto de spans relacionados que
describen una transacción distribuida.

**Sampler**                     Política de muestreo que decide cuántos
spans se envían.

**Batch Processor**             Agrupa spans antes de exportarlos,
reduciendo overhead.

**Collector**                   Servicio central que recibe, procesa y
reexporta datos de telemetry.

**Jaeger**                      Herramienta de visualización de trazas
distribuidas.

**Micrometer**                  Biblioteca de instrumentación para
métricas (Prometheus, Datadog, etc.).

**Prometheus**                  Sistema de scraping y almacenamiento de
métricas numéricas.

**Grafana**                     Plataforma de dashboards y
visualización.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## Validaciones finales

Verificación Resultado
  ---------------------------------------------------- -----------
Collector arranca sin errores ✅
Spans exportados (`Exporting N spans`)               ✅
Jaeger muestra `demo-payments`                       ✅
Prometheus scrapea `kotlin-app` y `otel-collector`   ✅
Grafana consulta métricas Prometheus ✅

------------------------------------------------------------------------

## Estructura de proyecto

    micrometer-otel-lab/
    ├── otel-collector-config.yaml
    ├── prometheus.yml
    ├── docker-compose.yml
    ├── data/
    │   ├── prometheus/
    │   └── grafana/
    └── src/
        └── main/kotlin/.../Application.kt

------------------------------------------------------------------------

## Próximos pasos

- Implementar spans manuales con el SDK (`Tracer.spanBuilder()`).
- Agregar un segundo microservicio y probar propagación de contexto.
- Incluir logs correlacionados (`trace_id`, `span_id`) con logback.
- Crear alertas Prometheus sobre métricas de fallas o latencias P95.

------------------------------------------------------------------------

## Aprendizajes técnicos clave

1. OpenTelemetry Java Agent simplifica la instrumentación sin modificar
   código.
2. Los pipelines del Collector son modulares y declarativos.
3. OTLP (gRPC) es el formato estándar de exportación moderno.
4. Micrometer y OTel son complementarios: métricas + trazas unificadas.
5. Jaeger ayuda a entender flujos distribuidos y dependencias.
6. Prometheus y Grafana permiten derivar SLOs a partir de métricas
   instrumentadas.

------------------------------------------------------------------------

**Autor:** Sebastian Páez\
**Fecha:** Noviembre 2025\
**Versión:** 1.0.0
