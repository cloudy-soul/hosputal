<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>Home</title>
    <jsp:include page="common/header.jsp" />
</head>
<body>
    <div class="container mt-5">
        <h1>Welcome to the Application!</h1>
        <p class="lead">This is your homepage. Feel free to explore.</p>
        <a href="/login" class="btn btn-primary">Login</a>
        <a href="/pharmacy/dashboard" class="btn btn btn-secondary">Go to Dashboard (if logged in)</a>
    </div>
    <jsp:include page="common/footer.jsp" />
</body>
</html>