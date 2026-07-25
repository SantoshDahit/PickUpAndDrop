package com.landgreet.repository.groupmessage;

import com.landgreet.entity.GroupMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMessageJpaRepository extends JpaRepository<GroupMessage, String> {

    List<GroupMessage> findAllByTravelGroupIdOrderByCreatedAtAsc(String groupId);
}
