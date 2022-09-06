<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%--<%@ taglib uri="http://www.springframework.org/security/tags" prefix="security" %>--%>

<%@ page language="java" contentType="text/html;charset=utf-8" pageEncoding="utf-8"%>

<%--<security:authentication property="principal" var="principal"/>--%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=8" />
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />

    <link href="<c:url value="/resources/ext/css/ext-all.css"/>" rel="stylesheet" type="text/css" />
    <link href="<c:url value="/resources/css/ux/ux-all.css"/>" rel="stylesheet" type="text/css" />
    <link href="<c:url value="/resources/css/css.css"/>" rel="stylesheet" type="text/css" />
    <link href="<c:url value="/resources/css/icons.css"/>" rel="stylesheet" type="text/css" />
    <style type="text/css">
        .action-disabled {
            cursor: default;
            opacity: 0;
            -moz-opacity: 0;
            filter: alpha(opacity=0);
        }
    </style>
    <script type="text/javascript" src="<c:url value="/scripts/framework/framework.js"/>" log="debug"></script>
    <script type="text/javascript">
        PROJECT_NAME = '调度系统';

        USER_ID = 1;
        USER_NAME = 'admin';
        USER_IS_ADMINISTRTOR = true;
        USER_IS_ETL = true;
        USER_GROUP_ID = 10;
        USER_GROUP_NAME = '系统管理员';
        USER_GROUP_IS_ADMINISTRTOR = true;

        _import([
            'com.sw.bi.scheduler.application.ApplicationModule',
            'com.sw.bi.scheduler.store-lib'
        ]);
    </script>
    <title>调度系统</title>

</head>
<body>
<iframe id="otheriframe" width="0" height="0" style="display:none;"></iframe>
</body>
</html>