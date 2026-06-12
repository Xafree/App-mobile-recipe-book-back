package fr.gymgod.common.service;

import fr.gymgod.common.entities.user.UserAccount;
import fr.gymgod.common.domain.user.UserAccountRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class SessionSpringService {

    private final UserAccountRepository userAccountRepository;

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No user authenticated");
        }
        return authentication.getName();
    }

    public UserAccount getCurrentUser() {
        String username = getCurrentUsername();
        return userAccountRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String getCurrentUserId() {
        HttpSession session = getSession();
        if (session != null) {
            String userId = (String) session.getAttribute("USER_ID");
            if (userId != null) {
                return userId;
            }
        }

        String username = getCurrentUsername();
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (session != null) {
            session.setAttribute("USER_ID", user.getId().toString());
        }

        return user.getId().toString();
    }

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attr != null ? attr.getRequest().getSession(false) : null;
    }
}
