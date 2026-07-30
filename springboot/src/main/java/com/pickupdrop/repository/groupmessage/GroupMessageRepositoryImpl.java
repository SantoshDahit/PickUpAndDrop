package com.pickupdrop.repository.groupmessage;

import com.pickupdrop.entity.GroupMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    @Override
    public Map<String, Long> countByGroupId() {
        return groupMessageJpaRepository.countGroupedByGroupId().stream()
                .collect(Collectors.toMap(GroupMessageCount::groupId, GroupMessageCount::total));
    }

    @Override
    public Map<String, GroupMessage> findLatestByGroupId() {
        // Merge keeps the later row so a createdAt tie resolves deterministically.
        return groupMessageJpaRepository.findLatestPerGroup().stream()
                .collect(Collectors.toMap(
                        message -> message.getTravelGroup().getId(),
                        Function.identity(),
                        (first, second) -> second,
                        LinkedHashMap::new));
    }
}
