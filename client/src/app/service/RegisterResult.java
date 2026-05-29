package app.service;

import app.model.User;

public record RegisterResult(boolean success, String code, User user) {}
