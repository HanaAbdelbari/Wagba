package com.wagba.users.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

//PATCH semantics: a null field means "leave unchanged".
public record UpdateProfileRequest(

        @Size(max = 150, message = "Full name must be at most 150 characters")
        @Pattern(regexp = "^(?!\\s*$).+$", message = "Full name must not be blank")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Phone number is not valid")
        String phone
) {
}