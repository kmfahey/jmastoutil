package org.kmfahey.jmastoutil.model;

import java.time.LocalDate;

public record Notification(
        String fromUserId, String toUserId, LocalDate createdAt, NotificationType notificationType, String statusUri
) { }