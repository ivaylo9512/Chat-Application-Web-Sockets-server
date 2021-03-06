package com.chat.app.controllers;

import com.chat.app.exceptions.PasswordsMissMatchException;
import com.chat.app.exceptions.UsernameExistsException;
import com.chat.app.models.Dtos.UserDto;
import com.chat.app.models.File;
import com.chat.app.models.UserDetails;
import com.chat.app.models.UserModel;
import com.chat.app.models.specs.RegisterSpec;
import com.chat.app.models.specs.UserSpec;
import com.chat.app.security.Jwt;
import com.chat.app.services.base.ChatService;
import com.chat.app.services.base.FileService;
import com.chat.app.services.base.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(value = "/api/users")
public class UserController {

    private final UserService userService;
    private final ChatService chatService;
    private final FileService fileService;

    public UserController(UserService userService, ChatService chatService, FileService fileService) {
        this.userService = userService;
        this.chatService = chatService;
        this.fileService = fileService;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(value = "auth/users/adminRegistration")
    public UserDto registerAdmin(@ModelAttribute RegisterSpec registerSpec, HttpServletResponse response){
        UserModel newUser = new UserModel(registerSpec, "ROLE_ADMIN");

        if(registerSpec.getProfileImage() != null){
            File profileImage = fileService.create(registerSpec.getProfileImage(), newUser.getId() + "logo");
            newUser.setProfileImage(profileImage);
        }

        String token = Jwt.generate(new UserDetails(newUser, new ArrayList<>(
                Collections.singletonList(new SimpleGrantedAuthority(newUser.getRole())))));
        response.addHeader("Authorization", "Token " + token);

        return new UserDto(userService.create(newUser));
    }

    @PostMapping(value = "/register")
    public UserDto register(@ModelAttribute RegisterSpec registerSpec, HttpServletResponse response) {
        UserModel newUser = new UserModel(registerSpec, "ROLE_USER");

        if(registerSpec.getProfileImage() != null){
            File profileImage = fileService.create(registerSpec.getProfileImage(), newUser.getId() + "logo");
            newUser.setProfileImage(profileImage);
        }

        String token = Jwt.generate(new UserDetails(newUser, new ArrayList<>(
                Collections.singletonList(new SimpleGrantedAuthority(newUser.getRole())))));
        response.addHeader("Authorization", "Token " + token);

        return new UserDto(userService.create(newUser));
    }

    @PostMapping("/login")
    public UserDto login(@RequestParam("pageSize") int pageSize){
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return new UserDto(loggedUser.getUserModel(), chatService.findUserChats(loggedUser.getId(), pageSize));
    }

    @GetMapping(value = "/findById/{id}")
    public UserDto findById(@PathVariable(name = "id") int id){
        return new UserDto(userService.findById(id));
    }

    @GetMapping(value = "/auth/searchForUsers/{username}")
    public List<UserDto> findByUsername(@PathVariable(name = "username") String username){
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getDetails();

        List<UserDto> userDtos = new ArrayList<>();
        userService.findByUsernameWithRegex(username).forEach(userModel -> {
            UserDto userDto = new UserDto(userModel);
            userDto.setHasChatWithLoggedUser(chatService.findIfUsersHaveChat(userModel.getId(), loggedUser.getId()));

            userDtos.add(userDto);
        });

        return userDtos;
    }

    @GetMapping(value = "/auth/getLoggedUser/{pageSize}")
    public UserDto getLoggedUser(@PathVariable("pageSize") int pageSize){
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getDetails();

        return new UserDto(userService.findById(loggedUser.getId()),
                chatService.findUserChats(loggedUser.getId(), pageSize));
    }

    @PostMapping(value = "/auth/changeUserInfo")
    public UserDto changeUserInfo(@RequestBody UserSpec userModel){
        UserDetails loggedUser = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getDetails();

        return new UserDto(userService.changeUserInfo(loggedUser.getId(), userModel));
    }

    @ExceptionHandler
    ResponseEntity handleUsernameExistsException(UsernameExistsException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }

    @ExceptionHandler
    ResponseEntity handlePasswordsMissMatchException(PasswordsMissMatchException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }
}
