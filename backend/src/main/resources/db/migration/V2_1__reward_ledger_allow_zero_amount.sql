alter table reward_ledger drop check chk_reward_ledger_amount;

alter table reward_ledger
    add constraint chk_reward_ledger_amount
        check (`amount` >= 0);
