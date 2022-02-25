package io.camunda.zeebe.process.test.filters.logger;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.process.test.filters.RecordStream;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.DeploymentRecordValue;
import io.camunda.zeebe.protocol.record.value.ErrorRecordValue;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.JobBatchRecordValue;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessEventRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceResultRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableDocumentRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.ProcessMetadataValue;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordStreamLogger {

  private static final Logger LOG = LoggerFactory.getLogger(RecordStreamLogger.class);

  private final RecordStream recordStream;
  private final Map<ValueType, Function<Record<?>, String>> valueTypeLoggers = new HashMap<>();

  public RecordStreamLogger(final RecordStreamSource recordStreamSource) {
    this.recordStream = RecordStream.of(recordStreamSource);
    valueTypeLoggers.put(ValueType.JOB, this::logJobRecordValue);
    valueTypeLoggers.put(ValueType.DEPLOYMENT, this::logDeploymentRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS_INSTANCE, this::logProcessInstanceRecordValue);
    valueTypeLoggers.put(ValueType.INCIDENT, this::logIncidentRecordValue);
    valueTypeLoggers.put(ValueType.MESSAGE, this::logMessageRecordValue);
    valueTypeLoggers.put(ValueType.MESSAGE_SUBSCRIPTION, this::logMessageSubscriptionRecordValue);
    valueTypeLoggers.put(
        ValueType.PROCESS_MESSAGE_SUBSCRIPTION, this::logProcessMessageSubscriptionRecordValue);
    valueTypeLoggers.put(ValueType.JOB_BATCH, this::logJobBatchRecordValue);
    valueTypeLoggers.put(ValueType.TIMER, this::logTimerRecordValue);
    valueTypeLoggers.put(
        ValueType.MESSAGE_START_EVENT_SUBSCRIPTION, this::logMessageStartEventSubscriptionRecordValue);
    valueTypeLoggers.put(ValueType.VARIABLE, this::logVariableRecordValue);
    valueTypeLoggers.put(ValueType.VARIABLE_DOCUMENT, this::logVariableDocumentRecordValue);
    valueTypeLoggers.put(
        ValueType.PROCESS_INSTANCE_CREATION, this::logProcessInstanceCreationRecordValue);
    valueTypeLoggers.put(ValueType.ERROR, this::logErrorRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS_INSTANCE_RESULT, this::logProcessInstanceResultRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS, this::logProcessRecordValue);
    valueTypeLoggers.put(ValueType.PROCESS_EVENT, this::logProcessEventRecordValue);

    // These records don't have any interesting extra information for the user to log
    valueTypeLoggers.put(ValueType.DEPLOYMENT_DISTRIBUTION, record -> "");
    valueTypeLoggers.put(ValueType.SBE_UNKNOWN, record -> "");
    valueTypeLoggers.put(ValueType.NULL_VAL, record -> "");
  }

  public void log() {
    final StringBuilder stringBuilder = new StringBuilder().append(System.lineSeparator());
    logRecords(stringBuilder);
  }

  private void logRecords(final StringBuilder stringBuilder) {
    stringBuilder.append("The following records have been recorded during this test:");
    recordStream
        .records()
        .forEach(
            record -> {
              stringBuilder.append(logGenericRecord(record));
              stringBuilder.append(
                  valueTypeLoggers
                      .getOrDefault(record.getValueType(), var -> "")
                      .apply(record));
            });

    LOG.info(stringBuilder.toString());
  }

  private String logGenericRecord(final Record<?> record) {
    return System.lineSeparator()
        + String.format("| %-20s", record.getRecordType())
        + String.format("%-35s", record.getValueType())
        + String.format("%-30s| ", record.getIntent());
  }

  private String logJobRecordValue(final Record<?> record) {
    final JobRecordValue value = (JobRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    // These fields are empty for commands
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Element id: %s)", value.getElementId()));
      joiner.add(String.format("(Job type: %s)", value.getType()));
      joiner.add(logVariables(value.getVariables()));
    }
    return joiner.toString();
  }

  private String logDeploymentRecordValue(final Record<?> record) {
    final DeploymentRecordValue value = (DeploymentRecordValue) record.getValue();
    final StringBuilder stringBuilder = new StringBuilder();
    if (!value.getResources().isEmpty()) {
      final StringJoiner joiner = new StringJoiner(", ", "[", "]");
      value.getResources().forEach(resource -> joiner.add(resource.getResourceName()));
      stringBuilder.append(String.format("(Processes: %s)", joiner));
    }
    return stringBuilder.toString();
  }

  private String logProcessInstanceRecordValue(final Record<?> record) {
    final ProcessInstanceRecordValue value = (ProcessInstanceRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Element id: %s)", value.getElementId()));
    joiner.add(String.format("(Element type: %s)", value.getBpmnElementType()));
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    return joiner.toString();
  }

  private String logIncidentRecordValue(final Record<?> record) {
    final IncidentRecordValue value = (IncidentRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Element id: %s)", value.getElementId()));
      joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    }
    return joiner.toString();
  }

  private String logMessageRecordValue(final Record<?> record) {
    final MessageRecordValue value = (MessageRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Message name: %s)", value.getName()));
    joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logMessageSubscriptionRecordValue(final Record<?> record) {
    final MessageSubscriptionRecordValue value = (MessageSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Message name: %s)", value.getMessageName()));
    joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logProcessMessageSubscriptionRecordValue(final Record<?> record) {
    final ProcessMessageSubscriptionRecordValue value =
        (ProcessMessageSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Message name: %s)", value.getMessageName()));

    // These fields are empty for commands
    if (record.getRecordType().equals(RecordType.EVENT)) {
      joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
      joiner.add(String.format("(Element id: %s)", value.getElementId()));
    }

    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logJobBatchRecordValue(final Record<?> record) {
    final JobBatchRecordValue value = (JobBatchRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Worker: %s)", value.getWorker()));
    joiner.add(String.format("(Job type: %s)", value.getType()));
    return joiner.toString();
  }

  private String logTimerRecordValue(final Record<?> record) {
    final TimerRecordValue value = (TimerRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Element id: %s)", value.getTargetElementId()));
    joiner.add(String.format("(Due date: %s)", new Date(value.getDueDate())));
    return joiner.toString();
  }

  private String logMessageStartEventSubscriptionRecordValue(final Record<?> record) {
    final MessageStartEventSubscriptionRecordValue value =
        (MessageStartEventSubscriptionRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    joiner.add(String.format("(Start event id: %s)", value.getStartEventId()));
    joiner.add(String.format("(Message name: %s)", value.getMessageName()));
    joiner.add(String.format("(Correlation key: %s)", value.getCorrelationKey()));
    return joiner.toString();
  }

  private String logVariableRecordValue(final Record<?> record) {
    final VariableRecordValue value = (VariableRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Name: %s)", value.getName()));
    joiner.add(String.format("(Value: %s)", value.getValue()));
    return joiner.toString();
  }

  private String logVariableDocumentRecordValue(final Record<?> record) {
    final VariableDocumentRecordValue value = (VariableDocumentRecordValue) record.getValue();
    return logVariables(value.getVariables());
  }

  private String logProcessInstanceCreationRecordValue(final Record<?> record) {
    final ProcessInstanceCreationRecordValue value =
        (ProcessInstanceCreationRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logErrorRecordValue(final Record<?> record) {
    final ErrorRecordValue value = (ErrorRecordValue) record.getValue();
    return String.format("(Exception message: %s)", value.getExceptionMessage());
  }

  private String logProcessInstanceResultRecordValue(final Record<?> record) {
    final ProcessInstanceResultRecordValue value =
        (ProcessInstanceResultRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Process id: %s)", value.getBpmnProcessId()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logProcessRecordValue(final Record<?> record) {
    final ProcessMetadataValue value = (ProcessMetadataValue) record.getValue();
    return String.format("(Process: %s)", value.getResourceName());
  }

  private String logProcessEventRecordValue(final Record<?> record) {
    final ProcessEventRecordValue value = (ProcessEventRecordValue) record.getValue();
    final StringJoiner joiner = new StringJoiner(", ", "", "");
    joiner.add(String.format("(Target element id: %s)", value.getTargetElementId()));
    joiner.add(logVariables(value.getVariables()));
    return joiner.toString();
  }

  private String logVariables(final Map<String, Object> variables) {
    if (variables.isEmpty()) {
      return "";
    }

    final StringJoiner joiner = new StringJoiner(", ", "[", "]");
    variables.forEach((key, value) -> joiner.add(key + " -> " + value.toString()));
    return String.format("(Variables: %s)", joiner);
  }

  protected Map<ValueType, Function<Record<?>, String>> getValueTypeLoggers() {
    return valueTypeLoggers;
  }
}