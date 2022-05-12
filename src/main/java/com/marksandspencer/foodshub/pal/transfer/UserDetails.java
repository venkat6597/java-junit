package com.marksandspencer.foodshub.pal.transfer;

import com.marksandspencer.assemblyservice.config.transfer.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetails {

    private UserRole userRole;
    private List<String> departments;
    private List<String> organizations;
    private String email;
}
