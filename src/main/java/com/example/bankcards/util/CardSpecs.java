package com.example.bankcards.util;

import com.example.bankcards.dto.CardAdminFilter;
import com.example.bankcards.entity.Card;
import org.springframework.data.jpa.domain.Specification;

public class CardSpecs {
    public static Specification<Card> byFilter(CardAdminFilter f) {
        return (root, q, cb) -> {
            var p = cb.conjunction();

            if (f.ownerId() != null) {
                p = cb.and(p, cb.equal(root.get("owner").get("id"), f.ownerId()));
            }
            if (f.email() != null && !f.email().isBlank()) {
                p = cb.and(p, cb.equal(cb.lower(root.get("owner").get("email")), f.email().toLowerCase()));
            }
            if (f.status() != null) {
                p = cb.and(p, cb.equal(root.get("status"), f.status()));
            }
            if (f.bin() != null && !f.bin().isBlank()) {
                p = cb.and(p, cb.equal(root.get("bin"), f.bin()));
            }
            if (f.panLast4() != null && !f.panLast4().isBlank()) {
                p = cb.and(p, cb.equal(root.get("panLast4"), f.panLast4()));
            }
            return p;
        };
    }
}

