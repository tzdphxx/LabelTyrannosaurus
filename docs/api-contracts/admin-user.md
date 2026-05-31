# Admin User API Contract

Owner: BE-B

## List Users

- URL: `GET /api/v1/admin/users?includeSystem=false`
- Roles: `ADMIN`
- Request: optional `includeSystem`; default `false`.
- Response: list of `userId`, `username`, `email`, `userType`, `enabled`, `loginEnabled`, `tokenVersion`, `role`.
- Effects: none.
- Errors: `401001` unauthenticated, `403001` non-admin.
- Frontend: admin user management page.

## Update Roles

- URL: `PUT /api/v1/admin/users/{userId}/roles`
- Roles: `ADMIN`
- Request: `role`
- Response: empty data.
- Effects: replaces the user's single `user_roles` row; increments `users.tokenVersion`.
- Errors: `400101` removing last `ADMIN`; `400102` missing user or invalid role; `401001`; `403001`.
- Frontend: admin user management page.

## Enable User

- URL: `POST /api/v1/admin/users/{userId}/enable`
- Roles: `ADMIN`
- Request: none.
- Response: empty data.
- Effects: sets `users.enabled=true` and increments `users.tokenVersion`.
- Errors: `400102` missing user; `401001`; `403001`.
- Frontend: admin user management page.

## Disable User

- URL: `POST /api/v1/admin/users/{userId}/disable`
- Roles: `ADMIN`
- Request: none.
- Response: empty data.
- Effects: sets `users.enabled=false` and increments `users.tokenVersion`; disabled users cannot log in.
- Errors: `400102` missing user; `401001`; `403001`.
- Frontend: admin user management page.
