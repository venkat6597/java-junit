package com.marksandspencer.foodshub.pal.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "PALAuditLogs")
@ToString
public class PALAuditLog {
    @Id
    private String id;
    private String productId;
    private List<Auditlog> auditLogs;
}
