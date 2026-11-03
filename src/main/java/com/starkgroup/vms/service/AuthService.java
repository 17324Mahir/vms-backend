package com.starkgroup.vms.service;

import com.starkgroup.vms.dto.LoginRequest;
import com.starkgroup.vms.dto.LoginResponse;

/**
 * Service managing user authentication and JWT issuance.
 */
public interface AuthService {
    LoginResponse login(LoginRequest request);
}
