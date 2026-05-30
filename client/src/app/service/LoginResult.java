package app.service;

import app.model.User;

public record LoginResult(boolean success, String code, User user) {}
