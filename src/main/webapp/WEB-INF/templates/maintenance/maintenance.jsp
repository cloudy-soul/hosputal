<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Maintenance</title>
    <jsp:include page="common/header.jsp" />
</head>
<body>
    <div class="container mt-5">
        <div class="alert alert-warning text-center" role="alert">
            <h4 class="alert-heading">Under Maintenance!</h4>
            <p>We're currently performing scheduled maintenance. We'll be back online shortly.</p>
            <hr>
            <p class="mb-0">Thank you for your patience.</p>
        </div>
        <p class="text-center"><a href="/" class="btn btn-primary">Go to Home</a></p>
    </div>
    <jsp:include page="common/footer.jsp" />
</body>
</html>
