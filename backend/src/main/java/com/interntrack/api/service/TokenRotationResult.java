package com.interntrack.api.service;

import com.interntrack.api.entity.User;

public record TokenRotationResult(User user, String rawRefreshToken) {
}