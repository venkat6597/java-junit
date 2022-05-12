package com.marksandspencer.foodshub.pal.domain;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Auditlog {
    private LocalDateTime auditDateTimeStamp;
    private String user;
    private String userName;
    private String auditField;
    private String auditFieldLabel;
    private String oldValue;
    private String newValue;
    private String userRole;
    private String userRoleLabel;
}
