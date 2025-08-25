package com.board.manager.config;

import com.board.manager.model.User;
import com.board.manager.service.BoardService;
import com.board.manager.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final BoardService boardService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private static final Pattern BOARD_TOPIC_PATTERN = Pattern.compile("/topic/board/(\\d+)");

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to carry the greeting messages back to the client
        config.enableSimpleBroker("/topic", "/queue");
        // Designates the "/app" prefix for messages that are bound for @MessageMapping-annotated methods
        config.setApplicationDestinationPrefixes("/app");
        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint, enabling SockJS fallback options
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new JwtAuthenticationInterceptor(), new BoardAccessChannelInterceptor());
    }

    /**
     * Interceptor to validate board access permissions on subscription
     */
    private class BoardAccessChannelInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                Principal user = accessor.getUser();

                if (destination != null && destination.startsWith("/topic/board/")) {
                    validateBoardAccess(destination, user, accessor.getSessionId());
                }
            }

            return message;
        }

        private void validateBoardAccess(String destination, Principal principal, String sessionId) {
            try {
                // Extract board ID from destination
                Matcher matcher = BOARD_TOPIC_PATTERN.matcher(destination);
                if (!matcher.matches()) {
                    log.warn("Invalid board topic format: {}", destination);
                    throw new AccessDeniedException("Invalid board topic format");
                }

                Integer boardId = Integer.parseInt(matcher.group(1));

                // Check authentication
                if (principal == null) {
                    log.warn("Unauthenticated user attempted to subscribe to board {} topic", boardId);
                    throw new AccessDeniedException("Authentication required for board access");
                }

                // Get user from principal
                User currentUser = getUserFromPrincipal(principal);

                // Validate board access permissions
                if (!boardService.canUserAccessBoard(boardId, currentUser)) {
                    log.warn("User {} denied subscription to board {} topic (session: {})",
                            currentUser.getUsername(), boardId, sessionId);
                    throw new AccessDeniedException("You do not have access to this board");
                }

                log.info("User {} granted subscription to board {} topic (session: {})",
                        currentUser.getUsername(), boardId, sessionId);

            } catch (NumberFormatException e) {
                log.error("Invalid board ID in destination {}: {}", destination, e.getMessage());
                throw new AccessDeniedException("Invalid board ID");
            } catch (Exception e) {
                log.error("Error validating board access for destination {}: {}", destination, e.getMessage());
                throw new AccessDeniedException("Board access validation failed: " + e.getMessage());
            }
        }

        private User getUserFromPrincipal(Principal principal) {
            if (principal instanceof Authentication authentication) {
                Object userPrincipal = authentication.getPrincipal();
                if (userPrincipal instanceof User user) {
                    return user;
                } else {
                    log.error("Invalid user principal type: {}", userPrincipal.getClass().getSimpleName());
                    throw new AccessDeniedException("Invalid user authentication");
                }
            }
            log.error("Principal is not an Authentication instance: {}", principal.getClass().getSimpleName());
            throw new AccessDeniedException("Invalid authentication type");
        }
    }

    /**
     * JWT Authentication interceptor for WebSocket connections
     */
    private class JwtAuthenticationInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authToken = accessor.getFirstNativeHeader("Authorization");

                if (authToken != null && authToken.startsWith("Bearer ")) {
                    try {
                        String jwt = authToken.substring(7);
                        String username = jwtService.extractUsername(jwt);

                        if (username != null) {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (jwtService.isTokenValid(jwt, userDetails)) {
                                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(authentication);
                                log.debug("WebSocket authenticated user: {}", username);
                            } else {
                                log.warn("Invalid JWT token for user: {}", username);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("WebSocket authentication failed: {}", e.getMessage());
                    }
                }
            }

            return message;
        }
    }
}
