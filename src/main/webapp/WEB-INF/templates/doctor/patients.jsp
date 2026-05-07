<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Patient List</title>
    <jsp:include page="../common/header.jsp" />
</head>
<body>
    <div class="container mt-5">
        <h2>Patient Directory</h2>
        <p>List of patients currently under your care.</p>
        <table class="table table-hover mt-3">
            <thead class="table-dark">
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Last Visit</th>
                    <th>Status</th>
                    <th>Action</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>P-1001</td>
                    <td>John Doe</td>
                    <td>2023-10-24</td>
                    <td><span class="badge bg-success">Stable</span></td>
                    <td><a href="#" class="btn btn-sm btn-primary">Details</a></td>
                </tr>
            </tbody>
        </table>
        <a href="/doctor/dashboard" class="btn btn-secondary mt-3">Back to Dashboard</a>
    </div>
    <jsp:include page="../common/footer.jsp" />
</body>
</html>