package com.cloudforge.invoice.delivery.service;

import com.cloudforge.invoice.delivery.domain.Role;

import java.util.Collection;

public interface RoleService {
    Role getRoleByUserId(Long id);
    Collection<Role> getRoles();
}
