package com.landgreet.service;

import com.landgreet.entity.GroupMessage;
import com.landgreet.repository.groupmessage.GroupMessageRepository;
import java.util.List;
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
}
