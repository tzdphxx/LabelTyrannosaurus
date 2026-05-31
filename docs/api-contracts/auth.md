# Auth API Contract

Owner: BE-B

## Register

- URL: `POST /api/v1/auth/register`
- Roles: public
- Request: `username`, `email`, `password`
- Response: `accessToken`, `refreshToken`, `tokenVersion`
- Effects: creates `users` row with `userType=USER`, `enabled=true`, `loginEnabled=true`, BCrypt `passwordHash`, `tokenVersion=1`; creates default `LABELER` role.
- Errors: `400102` duplicate username/email or invalid parameters.
- Frontend: register page.

## Login

- URL: `POST /api/v1/auth/login`
- Roles: public
- Request: `account`, `password`
- Response: `accessToken`, `refreshToken`, `tokenVersion`
- Effects: validates `enabled=true`, `loginEnabled=true`, password hash, and `userType=USER`; updates `lastLoginAt`.
- Errors: `401001` invalid credential, disabled user, login-disabled user, or system user.
- Frontend: login page.

## Refresh

- URL: `POST /api/v1/auth/refresh`
- Roles: public with valid refresh token
- Request: `refreshToken`
- Response: `accessToken`, `refreshToken`, `tokenVersion`
- Effects: validates refresh token and current `users.tokenVersion`.
- Errors: `401001` invalid token, expired token, stale `tokenVersion`, disabled user.
- Frontend: API client token refresh.

## Current User

- URL: `GET /api/v1/users/me`
- Roles: authenticated user
- Request: none
- Response: `userId`, `username`, `email`, `roles`
- Effects: none.
- Errors: `401001` unauthenticated or stale token.
- Frontend: auth state restore, permission menu.
