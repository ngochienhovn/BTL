package com.ltnc.auction.shared.dto;

public class UserDto {
    public Long id;
    public String fullName;
    public String email;
    public String role;

    public UserDto() {}

    public UserDto(Long id, String fullName, String email, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }
}
