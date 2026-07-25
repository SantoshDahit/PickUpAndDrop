package com.landgreet.repository.groupmessage;

import com.landgreet.entity.GroupMessage;
import java.util.List;

public interface GroupMessageRepository {

    GroupMessage save(GroupMessage message);

    List<GroupMessage> findAllByGroupId(String groupId);
}
