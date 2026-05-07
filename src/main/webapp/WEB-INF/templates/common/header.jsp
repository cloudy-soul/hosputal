<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand fw-bold" href="/">🏥 HMS Portal</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav" id="nav-links">
                <!-- populated by JS based on role -->
            </ul>
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <span class="nav-link text-white-50 small" id="nav-user-name"></span>
                </li>
                <li class="nav-item">
                    <button class="btn btn-sm btn-outline-light ms-2" onclick="doLogout()">Sign Out</button>
                </li>
            </ul>
        </div>
    </div>
</nav>
<script>
// Auth guard – redirect to login if no token
(function(){
    var token = sessionStorage.getItem('hms_token');
    var raw   = sessionStorage.getItem('hms_user');
    if (!token || !raw) { window.location.href = '/login'; return; }
    try {
        var user = JSON.parse(raw);
        var nameEl = document.getElementById('nav-user-name');
        if (nameEl) nameEl.textContent = (user.fullName || user.email) + ' (' + user.role + ')';

        // Build role-specific nav links
        var links = document.getElementById('nav-links');
        var roleLinks = {
            DOCTOR:     [['Doctor Dashboard','/doctor/dashboard'],['Patients','/doctor/patients']],
            PHARMACIST: [['Pharmacy Dashboard','/pharmacy/dashboard']],
            MAINTENANCE:[['Maintenance Dashboard','/maintenance/dashboard']],
            ADMIN:      [['Maintenance Dashboard','/maintenance/dashboard']],
            MANAGEMENT: [['Maintenance Dashboard','/maintenance/dashboard']]
        };
        var items = roleLinks[user.role] || [];
        links.innerHTML = items.map(function(l){
            return '<li class="nav-item"><a class="nav-link" href="'+l[1]+'">'+l[0]+'</a></li>';
        }).join('');
    } catch(e) { window.location.href = '/login'; }
})();

function doLogout() {
    sessionStorage.removeItem('hms_token');
    sessionStorage.removeItem('hms_user');
    window.location.href = '/login';
}

// Helper: make authenticated API calls from any JSP page
function apiGet(path) {
    return fetch(path, {
        headers: { 'Authorization': 'Bearer ' + sessionStorage.getItem('hms_token') }
    }).then(function(r){
        if (r.status === 401) { doLogout(); throw new Error('Unauthorised'); }
        return r.json();
    });
}
function apiPost(path, body) {
    return fetch(path, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + sessionStorage.getItem('hms_token')
        },
        body: JSON.stringify(body)
    }).then(function(r){
        if (r.status === 401) { doLogout(); throw new Error('Unauthorised'); }
        return r.status === 204 ? null : r.json();
    });
}
</script>
