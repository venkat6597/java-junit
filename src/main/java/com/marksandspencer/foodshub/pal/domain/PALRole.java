package com.marksandspencer.foodshub.pal.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "PALRole")
@ToString
public class PALRole {
    @Id
    private String id;
    private String objectId;
    private String name;
    private String role;
}
