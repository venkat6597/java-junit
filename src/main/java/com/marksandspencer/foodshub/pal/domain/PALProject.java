package com.marksandspencer.foodshub.pal.domain;

import com.marksandspencer.foodshub.pal.transfer.Personnel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "PALProject")
@ToString
public class PALProject {
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private String id;
    private String projectName;
    private String templateId;
    private String templateName;
    private String status;
    private String projectType;
    private String financialYear;
    private LocalDateTime projectCompletionDate;
    private String comments;
    private Personnel personnel;
    private LocalDateTime deletedDate;
}
