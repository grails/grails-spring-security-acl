<head>
<meta name="layout" content="main" />
<title>Report List</title>
</head>

<body>

<div class="body">

	<h1>Report List</h1>

	<g:if test="${flash.message}">
	<div class="message">${flash.message}</div>
	</g:if>

	<div class="list">
	<table>
	<thead>
		<tr>
			<g:sortableColumn property="id" title="ID" />
			<g:sortableColumn property="name" title="Name" />
		</tr>
	</thead>
	<tbody>
		<g:each in="${reportInstanceList}" status="i" var="reportInstance">
			<tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
				<td><g:link action="show" id="${reportInstance.id}">${reportInstance.id}</g:link></td>
				<td>${reportInstance.name}</td>
			</tr>
		</g:each>
	</tbody>
	</table>
	</div>

	<div class="paginateButtons">
	<g:paginate total="${reportInstanceTotal}" />
	</div>
</div>
</body>
