package io.camunda.zeebe.bpmnassert.filters;

import org.camunda.community.eze.RecordStreamSource;

public class StreamFilter {

  public static ProcessInstanceRecordStreamFilter processInstance(
      final RecordStreamSource recordStreamSource) {
    return new ProcessInstanceRecordStreamFilter(recordStreamSource.processInstanceRecords());
  }

  public static ProcessMessageSubscriptionRecordStreamFilter processMessageSubscription(
      final RecordStreamSource recordStreamSource) {
    return new ProcessMessageSubscriptionRecordStreamFilter(
        recordStreamSource.processMessageSubscriptionRecords());
  }

  public static VariableRecordStreamFilter variable(final RecordStreamSource recordStreamSource) {
    return new VariableRecordStreamFilter(recordStreamSource.variableRecords());
  }

  public static MessageRecordStreamFilter message(final RecordStreamSource recordStreamSource) {
    return new MessageRecordStreamFilter(recordStreamSource.messageRecords());
  }

  public static IncidentRecordStreamFilter incident(final RecordStreamSource recordStreamSource) {
    return new IncidentRecordStreamFilter(recordStreamSource.incidentRecords());
  }

  public static MessageStartEventSubscriptionStreamFilter messageStartEventSubscription(
      final RecordStreamSource recordStreamSource) {
    return new MessageStartEventSubscriptionStreamFilter(
        recordStreamSource.messageStartEventSubscriptionRecords());
  }

  public static ProcessEventRecordStreamFilter processEventRecords(
      final RecordStreamSource recordStreamSource) {
    return new ProcessEventRecordStreamFilter(recordStreamSource.records());
  }
}