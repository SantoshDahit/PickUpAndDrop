package com.pickupdrop.repository.groupmessage;

/** Per-group message total, for the admin chat index (plan 012 §4.3). */
public record GroupMessageCount(String groupId, long total) {
}
