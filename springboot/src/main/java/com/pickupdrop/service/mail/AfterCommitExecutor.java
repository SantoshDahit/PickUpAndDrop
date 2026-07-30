package com.pickupdrop.service.mail;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Defers a side effect until the surrounding transaction commits.
 *
 * <p>Mail must not go out for work that then rolls back — a "your pickup is
 * booked" email for a booking that never existed is worse than no email. With
 * no active transaction the task runs immediately.
 */
@Component
public class AfterCommitExecutor {

    public void execute(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
