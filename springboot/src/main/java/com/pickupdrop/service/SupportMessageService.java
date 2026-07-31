package com.pickupdrop.service;

import com.pickupdrop.entity.SupportMessage;
import com.pickupdrop.repository.supportmessage.SupportMessageRepository;
import com.pickupdrop.repository.supportmessage.SupportThreadRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupportMessageService {

    private final SupportMessageRepository supportMessageRepository;

    @Transactional(readOnly = true)
    public List<SupportMessage> getThread(String userId) {
        return supportMessageRepository.findThread(userId);
    }

    @Transactional(readOnly = true)
    public List<SupportThreadRow> getInbox() {
        return supportMessageRepository.findInbox();
    }

    @Transactional
    public SupportMessage save(SupportMessage message) {
        return supportMessageRepository.save(message);
    }

    /**
     * Marks the other side's messages read. {@code staff = true} clears the
     * team's messages (the traveller is reading); {@code false} clears the
     * traveller's (the operator is reading).
     */
    @Transactional
    public int markRead(String userId, boolean staff) {
        List<SupportMessage> unread = supportMessageRepository.findUnread(userId, staff);
        unread.forEach(SupportMessage::markRead);
        supportMessageRepository.saveAll(unread);
        return unread.size();
    }
}
