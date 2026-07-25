package com.pickupdrop.repository.groupmessage;

import com.pickupdrop.entity.GroupMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMessageJpaRepository extends JpaRepository<GroupMessage, String> {

    List<GroupMessage> findAllByTravelGroupIdOrderByCreatedAtAsc(String groupId);
}
