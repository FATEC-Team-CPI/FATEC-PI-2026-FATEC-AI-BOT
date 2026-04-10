package org.acme.repository;

import org.acme.model.Admin;

public interface IAdminRepository {
    Admin findByKeys(String pk, String sk);
}