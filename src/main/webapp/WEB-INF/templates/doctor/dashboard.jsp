<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Doctor Dashboard</title>
    <jsp:include page="../common/header.jsp" />
</head>
<body>
    <div class="container mt-4">
        <h2 class="mb-1">Doctor Dashboard</h2>
        <p class="text-muted">View patient records, schedules, and appointments.</p>

        <div class="alert alert-primary" id="welcome-msg">Loading…</div>

        <div class="row mt-4 g-3">
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">👤 Patients</h5>
                        <p class="card-text text-muted">View and update patient medical history.</p>
                        <a href="/doctor/patients" class="btn btn-outline-primary btn-sm">View Patients</a>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">📅 Appointments</h5>
                        <p class="card-text text-muted">Manage your daily appointments and availability.</p>
                        <button class="btn btn-outline-primary btn-sm" onclick="loadTodayAppointments()">Load Today</button>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title">📋 Prescriptions</h5>
                        <p class="card-text text-muted">Create and manage patient prescriptions.</p>
                        <button class="btn btn-outline-primary btn-sm" disabled>Coming soon</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Today's appointments table -->
        <div class="card mt-4 d-none" id="appt-card">
            <div class="card-header fw-semibold">Today's Appointments</div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead class="table-light">
                            <tr><th>Time</th><th>Patient</th><th>Reason</th><th>Status</th></tr>
                        </thead>
                        <tbody id="appt-tbody">
                            <tr><td colspan="4" class="text-center py-3 text-muted">Loading…</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <jsp:include page="../common/footer.jsp" />
    <script>
    // Set welcome message
    (function(){
        var user = JSON.parse(sessionStorage.getItem('hms_user') || '{}');
        var el = document.getElementById('welcome-msg');
        if (el) el.textContent = 'Welcome, Dr. ' + (user.fullName || user.email) + '!';
    })();

    function loadTodayAppointments() {
        document.getElementById('appt-card').classList.remove('d-none');
        var tbody = document.getElementById('appt-tbody');
        apiGet('/api/doctor/appointments/today')
            .then(function(data) {
                if (!data || !data.length) {
                    tbody.innerHTML = '<tr><td colspan="4" class="text-center py-3 text-muted">No appointments today.</td></tr>';
                    return;
                }
                tbody.innerHTML = data.map(function(a) {
                    return '<tr><td>' + (a.appointmentTime||'—') + '</td><td>' +
                        (a.patientName||'—') + '</td><td>' +
                        (a.reasonForVisit||'—') + '</td><td>' +
                        '<span class="badge bg-' + (a.status==='COMPLETED'?'success':'warning text-dark') + '">' +
                        a.status + '</span></td></tr>';
                }).join('');
            })
            .catch(function(e) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-3">Failed to load: ' + e.message + '</td></tr>';
            });
    }
    </script>
</body>
</html>
