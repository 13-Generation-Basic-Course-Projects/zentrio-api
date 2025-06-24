package org.example.zentrio.utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.zentrio.jwt.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String token = accessor.getFirstNativeHeader("Authorization");

            log.info("🔐 WebSocket CONNECT with Authorization: {}", token);

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
//                    String userId = jwtService.extractUserId(token);
                    // works for static user ID
                    String userId = "fdc53bff-b75e-4237-87b4-264b32888773";
                    // ✅ Parse and validate token
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userId,    // principal
                            null,      // credentials
                            Collections.emptyList() // authorities (roles)
                    );
                    accessor.setUser(authentication);

                    log.info("userID: {}", userId);

                    log.info("authentication: {}", authentication);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid JWT token", e);
                }
            }
        }
        return message;
    }
}

