package com.pickupdrop.repository.supportmessage;

import com.pickupdrop.entity.SupportMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SupportMessageRepositoryImpl implements SupportMessageRepository {

    private final SupportMessageJpaRepository supportMessageJpaRepository;

    @Override
    public List<SupportMessage> findThread(String userId) {
        return supportMessageJpaRepository.findAllByUserIdOrderByCreatedAtAsc(userId);
    }

    @Override
    public List<SupportThreadRow> findInbox() {
        return supportMessageJpaRepository.findInbox();
    }

    @Override
    public List<SupportMessage> findUnread(String userId, boolean staff) {
        return supportMessageJpaRepository.findUnread(userId, staff);
    }

    @Override
    public SupportMessage save(SupportMessage message) {
        return supportMessageJpaRepository.save(message);
    }

    @Override
    public List<SupportMessage> saveAll(List<SupportMessage> messages) {
        return supportMessageJpaRepository.saveAll(messages);
    }
}
