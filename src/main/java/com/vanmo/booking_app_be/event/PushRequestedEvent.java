package com.vanmo.booking_app_be.event;

import java.util.List;
import java.util.Map;

/** Published once notifications are stored; delivered to the recipients' devices after the transaction commits. */
public record PushRequestedEvent(List<String> accountIds, String title, String body, Map<String, String> data) {}
