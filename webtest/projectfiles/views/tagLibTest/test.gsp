<%@ page import="org.springframework.security.acls.domain.BasePermission" %>

<body>

	<g:each in='${[1L,13L,80L]}' var='reportId'>

		<%-- single String --%>
		<sec:permitted className='com.testacl.Report' id='${reportId}' permission='read'>
		test 1 true ${reportId}<br/>
		</sec:permitted>
		<sec:notPermitted className='com.testacl.Report' id='${reportId}' permission='read'>
		test 1 false ${reportId}<br/>
		</sec:notPermitted>

		<%-- multiple String --%>
		<sec:permitted className='com.testacl.Report' id='${reportId}' permission='write,read'>
		test 2 true ${reportId}<br/>
		</sec:permitted>
		<sec:notPermitted className='com.testacl.Report' id='${reportId}' permission='read,write'>
		test 2 false ${reportId}<br/>
		</sec:notPermitted>

		<%-- single Permission --%>
		<sec:permitted className='com.testacl.Report' id='${reportId}' permission='${BasePermission.READ}'>
		test 3 true ${reportId}<br/>
		</sec:permitted>
		<sec:notPermitted className='com.testacl.Report' id='${reportId}' permission='${BasePermission.READ}'>
		test 3 false ${reportId}<br/>
		</sec:notPermitted>

		<%-- List of Permission --%>
		<sec:permitted className='com.testacl.Report' id='${reportId}' permission='${[BasePermission.WRITE,BasePermission.READ]}'>
		test 4 true ${reportId}<br/>
		</sec:permitted>
		<sec:notPermitted className='com.testacl.Report' id='${reportId}' permission='${[BasePermission.WRITE,BasePermission.READ]}'>
		test 4 false ${reportId}<br/>
		</sec:notPermitted>

		<%-- single mask int --%>
		<sec:permitted className='com.testacl.Report' id='${reportId}' permission='${1}'>
		test 5 true ${reportId}<br/>
		</sec:permitted>
		<sec:notPermitted className='com.testacl.Report' id='${reportId}' permission='${1}'>
		test 5 false ${reportId}<br/>
		</sec:notPermitted>

		<%-- multiple mask int --%>
		<sec:permitted className='com.testacl.Report' id='${reportId}' permission='2,1'>
		test 6 true ${reportId}<br/>
		</sec:permitted>
		<sec:notPermitted className='com.testacl.Report' id='${reportId}' permission='2,1'>
		test 6 false ${reportId}<br/>
		</sec:notPermitted>

		<br/>

	</g:each>

</body>
