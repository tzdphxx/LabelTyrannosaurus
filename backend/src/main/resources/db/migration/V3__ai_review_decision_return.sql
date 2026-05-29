alter table ai_review_results
    drop check chk_ai_review_results_decision;

alter table ai_review_results
    add constraint chk_ai_review_results_decision
        check ((`decision` is null) or (`decision` in (_utf8mb4'PASS', _utf8mb4'RETURN', _utf8mb4'MANUAL_REVIEW')));
