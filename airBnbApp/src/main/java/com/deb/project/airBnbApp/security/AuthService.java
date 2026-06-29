package com.deb.project.airBnbApp.security;

import com.deb.project.airBnbApp.dto.LoginDto;
import com.deb.project.airBnbApp.dto.SignUpRequestDto;
import com.deb.project.airBnbApp.dto.UserDto;
import com.deb.project.airBnbApp.entity.User;
import com.deb.project.airBnbApp.entity.enums.Role;
import com.deb.project.airBnbApp.exception.ResourceNotFoundException;
import com.deb.project.airBnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserDto signUp(SignUpRequestDto signUpRequestDto){

        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);

        if (user!=null){
            throw new RuntimeException("User already exists");
        }

        User newUser=modelMapper.map(signUpRequestDto,User.class);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));

        newUser=userRepository.save(newUser);

        return modelMapper.map(newUser, UserDto.class);
    }

    public String[] login(LoginDto loginDto){
        Authentication authentication=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(),loginDto.getPassword()
        ));

        User user=(User) authentication.getPrincipal();
        String arr[] = new String[2];
        arr[0]= jwtService.generateAccessToken(user);
        arr[1]= jwtService.generateRefreshToken(user);

        log.info("user with id: "+user.getId());

        return arr;
    }

    public String refreshToken(String refreshToken) {
        Long id = jwtService.generateUserIdFromToken(refreshToken);

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: "+id));
        return jwtService.generateAccessToken(user);
    }
}
