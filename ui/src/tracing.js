import { WebTracerProvider, ConsoleSpanExporter, SimpleSpanProcessor } from '@opentelemetry/sdk-trace-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import { XMLHttpRequestInstrumentation } from '@opentelemetry/instrumentation-xml-http-request';
import { registerInstrumentations } from '@opentelemetry/instrumentation';

const provider = new WebTracerProvider();

// Configure the OTLP exporter to send traces to your collector
const exporter = new OTLPTraceExporter({
  url: 'http://localhost:4318/v1/traces', // Default OTLP HTTP endpoint
});

provider.addSpanProcessor(new SimpleSpanProcessor(exporter));

// Register instrumentations
registerInstrumentations({
  instrumentations: [
    new FetchInstrumentation(),
    new XMLHttpRequestInstrumentation(),
  ],
});

provider.register();

console.log('OpenTelemetry tracing initialized');
