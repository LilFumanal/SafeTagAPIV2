package com.lil.safetag.dto;

public class PractitionnerDto {
    private String rppsId;
    private String firstName;
    private String lastName;
    private String profession;
    private String specialty;
    private String[] address;

    public PractitionnerDto(String rppsId, String firstName, String lastName, String profession, String specialty, String[] address) {
        this.rppsId = rppsId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profession = profession;
        this.specialty = specialty;
        this.address = address;
    }


    public String getRppsId() {
        return rppsId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getProfession() {
        return profession;
    }

    public String getSpecialty() {
        return specialty;
    }

    public String[] getAddress() {
        return address;
    }
}
