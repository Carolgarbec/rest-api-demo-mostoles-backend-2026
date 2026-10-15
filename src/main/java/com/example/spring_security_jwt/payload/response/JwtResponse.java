package com.example.spring_security_jwt.payload.response;



import java.util.Set;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class JwtResponse {
	
	private String token;
	private final String  type = "Bearer";
	private long id;
	private String username;
	private String email;
	private Set<String> roles;

}
