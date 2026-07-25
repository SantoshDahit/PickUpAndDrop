package com.landgreet.repository.groupmessage;

import com.landgreet.entity.GroupMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GroupMessageRepositoryImpl implements GroupMessageRepository {

    private final GroupMessageJpaRepository groupMessageJpaRepository;

    @Override
    public GroupMessage save(GroupMessage message) {
        return groupMessageJpaRepository.save(message);
    }

    @Override
    public List<GroupMessage> findAllByGroupId(String groupId) {
        return groupMessageJpaRepository.findAllByTravelGroupIdOrderByCreatedAtAsc(groupId);
    }
}
