package com.pickupdrop.service;

import com.pickupdrop.entity.GroupMessage;
import com.pickupdrop.repository.groupmessage.GroupMessageRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;

    @Transactional
    public GroupMessage save(GroupMessage message) {
        return groupMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<GroupMessage> getAllByGroupId(String groupId) {
        return groupMessageRepository.findAllByGroupId(groupId);
    }

    /** Totals per group for the admin chat index — one query, not one per row. */
    @Transactional(readOnly = true)
    public Map<String, Long> countByGroupId() {
        return groupMessageRepository.countByGroupId();
    }

    /** Newest message per group, for the admin chat index preview. */
    @Transactional(readOnly = true)
    public Map<String, GroupMessage> getLatestByGroupId() {
        return groupMessageRepository.findLatestByGroupId();
    }
}
