package sovok.mcbuildlibrary.model;

import org.springframework.security.core.GrantedAuthority;

/**
 * Represents user roles in the application.
 * Implements GrantedAuthority for Spring Security integration.
 */
public enum Role implements GrantedAuthority {
  /**
   * Standard user role with limited permissions.
   * Can view/download builds and create new builds under their name.
   */
  ROLE_USER,

  /**
   * Administrator role with full permissions.
   * Can perform any action in the system.
   */
  ROLE_ADMIN;

    @Override
    public String getAuthority() {
        return name();
    }
}