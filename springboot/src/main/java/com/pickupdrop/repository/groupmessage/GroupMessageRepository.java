package com.pickupdrop.repository.groupmessage;

import com.pickupdrop.entity.GroupMessage;
import java.util.List;
import java.util.Map;

public interface GroupMessageRepository {

    GroupMessage save(GroupMessage message);

    List<GroupMessage> findAllByGroupId(String groupId);

    /** Message totals keyed by group id — one query for the whole admin index. */
    Map<String, Long> countByGroupId();

    /** Newest message per group, keyed by group id, authors already loaded. */
    Map<String, GroupMessage> findLatestByGroupId();
}
