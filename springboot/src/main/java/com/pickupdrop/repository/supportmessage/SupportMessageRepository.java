package com.pickupdrop.repository.supportmessage;

import com.pickupdrop.entity.SupportMessage;
import java.util.List;

public interface SupportMessageRepository {

    List<SupportMessage> findThread(String userId);

    /** One row per traveller who has written, newest activity first. */
    List<SupportThreadRow> findInbox();

    /** The other side's still-unread messages in this thread. */
    List<SupportMessage> findUnread(String userId, boolean staff);

    SupportMessage save(SupportMessage message);

    List<SupportMessage> saveAll(List<SupportMessage> messages);
}
