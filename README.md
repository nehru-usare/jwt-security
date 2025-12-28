This document is written from a **Spring Security framework engineer’s perspective**, not a tutorial.

---

# JWT + Spring Boot Security

## Complete Request Flow – Deep Dive (Login & API Access)

---

## 0. Purpose of This Document

This document explains **how Spring Security actually works internally** when JWT is used in a **stateless Spring Boot application**.

This is **not** about:

* “How to add JWT”
* “Which annotations to use”

This **is** about:

* How authentication really happens
* How authorization really happens
* What each filter does
* How `SecurityContext` works without a session
* Why JWT changes the entire mental model

After reading this, you should be able to:

* Visualize the **entire request lifecycle**
* Debug Spring Security without guesswork
* Explain JWT security confidently in interviews

---

## 1. Core Mental Model Shift (CRITICAL)

### HTTP Basic / Session-Based Security

```
Login
 → Authentication
 → HttpSession created
 → Session ID stored in cookie
 → Server remembers user
```

* Server is **stateful**
* Authentication state lives on server
* Session = memory

---

### JWT-Based Security

```
Login
 → Authentication
 → JWT issued
 → Client stores JWT
 → Server forgets everything
```

* Server is **stateless**
* Authentication state lives in the **token**
* JWT = portable proof of identity

> 🔑 **JWT does NOT authenticate users**
> JWT proves authentication already happened

---

## 2. Key Spring Security Concepts (Internal)

### 2.1 Authentication vs Authorization

| Concept        | Meaning                       |
| -------------- | ----------------------------- |
| Authentication | “Who are you?”                |
| Authorization  | “What are you allowed to do?” |

JWT is used **only for authorization** after login.

---

### 2.2 SecurityContext (Very Important)

* `SecurityContext` holds the current `Authentication`
* Stored in a **ThreadLocal**
* Exists **per request**
* **NOT** an HTTP session

```
Thread
 └── SecurityContextHolder
      └── SecurityContext
           └── Authentication
```

Stateless ≠ Context-less
Context is rebuilt **every request**

---

## 3. Spring Security Filter Chain (Reality)

Every request passes through a **fixed, ordered filter chain**.

Relevant filters for JWT:

```
SecurityContextHolderFilter
↓
LoginRateLimitFilter
↓
JwtAuthorizationFilter
↓
AnonymousAuthenticationFilter
↓
ExceptionTranslationFilter
↓
FilterSecurityInterceptor
```

Order is **non-negotiable**.

---

## 4. LOGIN FLOW (JSON → JWT)

### Example Request

```http
POST /login
Content-Type: application/json

{
  "usernameOrEmail": "admin",
  "password": "admin123"
}
```

---

### 4.1 Request Enters Server

* Tomcat assigns a thread
  Example:

```
http-nio-8080-exec-1
```

* No session
* No authentication
* Empty `SecurityContext`

---

### 4.2 Security Filter Chain (Login)

Key facts:

* `/login` is `permitAll`
* JWT filter is skipped
* No authorization enforced yet

---

### 4.3 Controller Boundary (`AuthController`)

The controller **does NOT authenticate manually**.

It delegates to Spring Security.

```java
authenticationManager.authenticate(...)
```

This is the **only correct way**.

---

### 4.4 AuthenticationManager (Internal)

`AuthenticationManager` is actually a `ProviderManager`.

```
AuthenticationManager
 → DaoAuthenticationProvider
```

It loops through providers until one can authenticate.

---

### 4.5 DaoAuthenticationProvider (Real Authentication)

#### Step 1: Load User

```java
UserDetailsService.loadUserByUsername()
```

* Fetches user from DB
* Converts entity → `UserDetails`

Failure:

```
UsernameNotFoundException → 401
```

---

#### Step 2: Password Verification

```java
BCryptPasswordEncoder.matches(raw, hash)
```

* Salt extracted from hash
* Hash recomputed
* Constant-time comparison

Failure:

```
BadCredentialsException → 401
```

---

#### Step 3: Authentication Success

Spring Security creates:

```java
UsernamePasswordAuthenticationToken(
  principal,
  null,
  authorities
)
```

At this point:

```java
authentication.isAuthenticated() == true
```

---

### 4.6 JWT Generation

The controller calls:

```java
jwtUtil.generateToken(user)
```

JWT contains:

* `sub` → username
* `roles` → ROLE_ADMIN, ROLE_USER
* `iat`
* `exp`
* `iss`
* HMAC-SHA256 signature

No DB access
No session
Fully stateless

---

### 4.7 Login Response

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

Thread ends.
Server remembers **nothing**.

---

## 5. API ACCESS FLOW (JWT → Authorization)

### Example Request

```http
GET /api/admin
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

### 5.1 Request Enters Server

* New thread
* Empty `SecurityContext`
* No session lookup

---

### 5.2 SecurityContextHolderFilter

* Creates empty `SecurityContext`
* Binds it to ThreadLocal

---

### 5.3 JwtAuthorizationFilter (Core of JWT)

#### Step 1: Extract Token

```java
Authorization: Bearer <token>
```

If missing → skip

---

#### Step 2: Validate Token

```java
jwtUtil.validateToken(token)
```

Validations:

* Signature
* Expiration
* Issuer
* Structure

Failure → `JwtException` → 401

---

#### Step 3: Rebuild Authentication

```java
Authentication auth =
  new UsernamePasswordAuthenticationToken(
    username,
    null,
    authorities
  );
```

---

#### Step 4: Populate SecurityContext

```java
SecurityContextHolder
  .getContext()
  .setAuthentication(auth);
```

Now the request **has identity**.

---

### 5.4 AnonymousAuthenticationFilter

Checks:

```java
if (authentication == null)
```

But JWT already populated it → skipped.

---

### 5.5 ExceptionTranslationFilter

Does nothing on success.
Acts as a safety net for:

* 401
* 403

---

### 5.6 FilterSecurityInterceptor (Authorization)

Evaluates rules from `SecurityConfig`.

Example:

```java
.hasRole("ADMIN")
```

Internally:

```java
authentication.getAuthorities()
  .contains("ROLE_ADMIN")
```

Outcomes:

| Condition                    | Result  |
| ---------------------------- | ------- |
| Authenticated + correct role | Allowed |
| Authenticated + wrong role   | 403     |
| Not authenticated            | 401     |

---

### 5.7 Controller Execution

Only now does Spring MVC call:

```java
TestController.admin()
```

The controller **trusts** the framework.

---

### 5.8 Response Exit

* Response returned
* Thread released
* `SecurityContext` cleared

No memory leak
No identity persistence

---

## 6. Failure Scenarios (Mapped Internally)

| Scenario                | Where It Fails            | Result |
| ----------------------- | ------------------------- | ------ |
| No token                | FilterSecurityInterceptor | 401    |
| Invalid token           | JwtAuthorizationFilter    | 401    |
| Expired token           | JwtAuthorizationFilter    | 401    |
| Wrong role              | FilterSecurityInterceptor | 403    |
| Too many login attempts | RateLimitFilter           | 429    |

---

## 7. Why This Design Is Correct

### Stateless but Secure

* No session fixation
* Horizontal scaling works
* Zero server memory per user

### Framework-Aligned

* No bypassing Spring Security
* No manual auth hacks
* Exceptions handled centrally

### Production-Ready

* Rate limiting
* Structured responses
* Clear boundaries

---

## 8. One-Paragraph Interview Explanation

> “In a JWT-based Spring Security system, authentication happens once during login via AuthenticationManager. The resulting identity is cryptographically signed into a JWT and returned to the client. For every subsequent request, a custom filter validates the token, reconstructs the Authentication object, and stores it in a thread-local SecurityContext. Authorization is enforced by FilterSecurityInterceptor based on roles, without using sessions or server-side state.”

---

## 9. Final Mental Model (Memorize This)

### Login

```
Credentials → Authentication → JWT
```

### API Access

```
JWT → Authentication (reconstructed) → Authorization
```

JWT replaces **server memory**, not **Spring Security**.

---

## End of Document
