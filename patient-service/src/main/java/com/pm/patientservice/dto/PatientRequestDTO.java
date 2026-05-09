package com.pm.patientservice.dto;

import com.pm.patientservice.validators.CreatePatientValidationGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientRequestDTO {
  @NotBlank(message = "Name is Required")
  @Size(max = 100, message = "Name cannot exceed 100 characters")
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Email not Valid")
  private String email;

  @NotBlank(message = "Address is required")
  private String address;

  @NotBlank(message = "DOB is required")
  private String dateOfBirth;

  @NotBlank(groups = CreatePatientValidationGroup.class, message = "Registered date is required")
  private String registeredDate;
}
