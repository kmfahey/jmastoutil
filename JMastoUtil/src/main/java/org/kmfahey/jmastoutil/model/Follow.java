package org.kmfahey.jmastoutil.model;

import java.time.LocalDate;

public record Follow(String byUserId, String ofUserId, LocalDate lastEvent, FollowRelationType relationType) { }