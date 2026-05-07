<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Hospital Management - Login</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { background: #0D1B3E; min-height: 100vh; display: flex; align-items: center; justify-content: center; }
        .login-card { max-width: 420px; width: 100%; border-radius: 12px; overflow: hidden; }
        .login-header { background: #1A2F5E; padding: 28px; text-align: center; }
        .login-header .icon { font-size: 48px; }
    </style>
</head>
<body>

<div class="login-card shadow-lg">
    <div class="login-header text-white">
        <div class="icon">🏥</div>
        <h4 class="mt-2 mb-0 fw-bold">Hospital Management</h4>
        <p class="text-white-50 small mb-0">Secure departmental access</p>
    </div>
    <div class="card-body bg-white p-4">

        <div id="login-alert" class="alert alert-danger d-none" role="alert"></div>

        <div class="mb-3">
            <label for="email" class="form-label fw-medium small">Email address</label>
            <input type="email" class="form-control" id="email"
                   placeholder="you@hospital.com" autocomplete="username">
        </div>
        <div class="mb-3">
            <label for="password" class="form-label fw-medium small">Password</label>
            <input type="password" class="form-control" id="password"
                   placeholder="••••••••" autocomplete="current-password">
        </div>

        <button id="btn-login" class="btn btn-primary w-100 py-2 mt-1 fw-semibold">
            Sign In
        </button>

        <p class="text-center text-muted small mt-3 mb-0">
            Contact IT support if you cannot access your account.
        </p>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
(function () {
    // If already logged in, redirect to the right dashboard
    var token = sessionStorage.getItem('hms_token');
    var raw   = sessionStorage.getItem('hms_user');
    if (token && raw) {
        try { redirectByRole(JSON.parse(raw).role); } catch(e) {}
    }

    function redirectByRole(role) {
        var routes = {
            DOCTOR:      '/doctor/dashboard',
            PHARMACIST:  '/pharmacy/dashboard',
            MAINTENANCE: '/maintenance/dashboard',
            ADMIN:       '/maintenance/dashboard',
            MANAGEMENT:  '/maintenance/dashboard'
        };
        window.location.href = routes[role] || '/';
    }

    document.getElementById('btn-login').addEventListener('click', doLogin);
    document.getElementById('password').addEventListener('keydown', function(e) {
        if (e.key === 'Enter') doLogin();
    });

    async function doLogin() {
        var btn   = document.getElementById('btn-login');
        var alert = document.getElementById('login-alert');
        var email = document.getElementById('email').value.trim();
        var pass  = document.getElementById('password').value;

        alert.classList.add('d-none');

        if (!email || !pass) {
            alert.textContent = 'Please enter your email and password.';
            alert.classList.remove('d-none');
            return;
        }

        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Signing in…';

        try {
            // POST /api/auth/login → { token, email, role, fullName }
            var res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, password: pass })
            });

            if (!res.ok) {
                var err = 'Invalid credentials. Please try again.';
                try { var j = await res.json(); err = j.message || err; } catch(e) {}
                throw new Error(err);
            }

            var data = await res.json();

            // Store JWT and user info in sessionStorage
            sessionStorage.setItem('hms_token', data.token);
            sessionStorage.setItem('hms_user', JSON.stringify({
                email:    data.email,
                role:     data.role,
                fullName: data.fullName
            }));

            redirectByRole(data.role);

        } catch (err) {
            alert.textContent = err.message || 'Login failed. Please try again.';
            alert.classList.remove('d-none');
            btn.disabled = false;
            btn.textContent = 'Sign In';
        }
    }
})();
</script>
</body>
</html>
