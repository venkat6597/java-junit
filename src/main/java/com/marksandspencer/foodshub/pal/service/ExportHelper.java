package com.marksandspencer.foodshub.pal.service;

import com.marksandspencer.foodshub.pal.domain.PALProject;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.transfer.PALProductResponse;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;

public interface ExportHelper {
    ByteArrayResource createExcel(List<PALProductResponse> palProductResponses,
                                  List<PALRole> roles, PALProject palProject);

    ByteArrayResource createExcelwithMacro(String userRole, List<PALProductResponse> palProductResponses,
                                           List<PALRole> roles, PALProject palProject, String fileName);
}
