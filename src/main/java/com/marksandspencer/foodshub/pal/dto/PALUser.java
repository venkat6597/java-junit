package com.marksandspencer.foodshub.pal.dto;

import lombok.*;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PALUser {

    private String id;
    private String name;
    private String email;
}
