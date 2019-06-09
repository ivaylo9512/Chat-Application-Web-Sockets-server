package com.chat.app.controllers;

import com.chat.app.models.DTOs.ChatDto;
import com.chat.app.models.UserDetails;
import com.chat.app.services.base.ChatService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@Controller
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/getChats")
    public List<ChatDto> getChats(@RequestParam(name = "pageSize") int pageSize){

        UserDetails userDetails = (UserDetails)SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getDetails();
        int userId = userDetails.getId();

        return chatService.findUserChats(userId, pageSize);

    }

    @PostMapping("auth/create")
    public ChatDto createChat(@RequestParam("userId") int requestedUserId){
        UserDetails loggedUser = (UserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getDetails();
        int loggedUserId = loggedUser.getId();

        return chatService.createChat(loggedUserId, requestedUserId);
    }

}