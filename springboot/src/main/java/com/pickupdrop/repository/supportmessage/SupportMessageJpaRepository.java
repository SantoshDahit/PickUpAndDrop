package com.pickupdrop.repository.supportmessage;

import com.pickupdrop.entity.SupportMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupportMessageJpaRepository extends JpaRepository<SupportMessage, String> {

    List<SupportMessage> findAllByUserIdOrderByCreatedAtAsc(String userId);

    /**
     * The operator's inbox: one row per traveller who has written, with totals
     * and how many of their messages are still unread. Soft-deleted accounts are
     * excluded — there is nobody left to reply to.
     */
    @Query("""
           select new com.pickupdrop.repository.supportmessage.SupportThreadRow(
                      u.id, u.name, u.email, u.phone,
                      count(m),
                      sum(case when m.staff = false and m.readAt is null then 1 else 0 end),
                      max(m.createdAt))
             from SupportMessage m
             join m.user u
            where u.deletedAt is null
            group by u.id, u.name, u.email, u.phone
            order by max(m.createdAt) desc
           """)
    List<SupportThreadRow> findInbox();

    /** Mark the other side's messages read. {@code staff} selects whose. */
    @Query("""
           select m from SupportMessage m
            where m.user.id = :userId
              and m.staff = :staff
              and m.readAt is null
           """)
    List<SupportMessage> findUnread(@Param("userId") String userId, @Param("staff") boolean staff);
}
