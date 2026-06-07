# Auth API Contract

Owner: BE-B

## Register

- Description: Creates a normal user account and immediately returns login tokens for the selected business role.
- URL: `POST /api/v1/auth/register`
- Roles: public
- Request: `username`, `email`, `password`, `role` (`LABELER`, `OWNER`, or `REVIEWER`)
- Response: `accessToken`, `refreshToken`, `tokenVersion`, `role`
- Effects: creates `users` row with `userType=USER`, `enabled=true`, `loginEnabled=true`, BCrypt `passwordHash`, `tokenVersion=1`; creates selected `user_roles` row.
- Errors: `400102` duplicate username/email, missing role, unsupported role, or invalid parameters.
- Frontend: register page.

## Login

- Description: Authenticates a user by account and password, then issues fresh access and refresh tokens.
- URL: `POST /api/v1/auth/login`
- Roles: public
- Request: `account`, `password`
- Response: `accessToken`, `refreshToken`, `tokenVersion`, `role`
- Effects: validates `enabled=true`, `loginEnabled=true`, password hash, and `userType=USER`; updates `lastLoginAt`.
- Errors: `401001` invalid credential, disabled user, login-disabled user, or system user.
- Frontend: login page.

## Refresh

- Description: Exchanges a valid refresh token for a new token pair while enforcing token version freshness.
- URL: `POST /api/v1/auth/refresh`
- Roles: public with valid refresh token
- Request: `refreshToken`
- Response: `accessToken`, `refreshToken`, `tokenVersion`, `role`
- Effects: validates refresh token and current `users.tokenVersion`.
- Errors: `401001` invalid token, expired token, stale `tokenVersion`, disabled user.
- Frontend: API client token refresh.

## Current User

- Description: Returns the authenticated user's identity and active role for frontend session restoration.
- URL: `GET /api/v1/users/me`
- Roles: authenticated user
- Request: none
- Response: `userId`, `username`, `email`, `role`
- Effects: none.
- Errors: `401001` unauthenticated or stale token.
- Frontend: auth state restore, permission menu.
