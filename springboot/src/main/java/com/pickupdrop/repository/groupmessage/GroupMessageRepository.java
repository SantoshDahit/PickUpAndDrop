package com.pickupdrop.repository.groupmessage;

import com.pickupdrop.entity.GroupMessage;
import java.util.List;

public interface GroupMessageRepository {

    GroupMessage save(GroupMessage message);

    List<GroupMessage> findAllByGroupId(String groupId);
}
