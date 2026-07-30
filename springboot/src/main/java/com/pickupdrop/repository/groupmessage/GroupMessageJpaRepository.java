package com.pickupdrop.repository.groupmessage;

import com.pickupdrop.entity.GroupMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupMessageJpaRepository extends JpaRepository<GroupMessage, String> {

    List<GroupMessage> findAllByTravelGroupIdOrderByCreatedAtAsc(String groupId);

    /**
     * Message totals for every group in one query — the admin index needs a
     * count per row and must not issue one query per group.
     */
    @Query("""
           select new com.pickupdrop.repository.groupmessage.GroupMessageCount(
                      m.travelGroup.id, count(m))
             from GroupMessage m
            group by m.travelGroup.id
           """)
    List<GroupMessageCount> countGroupedByGroupId();

    /**
     * The newest message of each group, author fetched, in one query. A tie on
     * {@code createdAt} inside one group yields two rows for it; the caller
     * keeps the last, which is equally valid for a preview.
     */
    @Query("""
           select m
             from GroupMessage m
             join fetch m.user
            where m.createdAt = (select max(m2.createdAt)
                                   from GroupMessage m2
                                  where m2.travelGroup.id = m.travelGroup.id)
            order by m.createdAt asc
           """)
    List<GroupMessage> findLatestPerGroup();
}
