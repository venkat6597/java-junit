package com.marksandspencer.foodshub.pal.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marksandspencer.foodshub.pal.transfer.Personnel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "PALProduct")
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PALProduct {
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private String id;
    private String templateId;
    private String projectId;
    private Personnel personnel;
    private List<DataField> datafields;
    private LocalDateTime deletedDate;
}
