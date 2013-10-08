<head>
	<meta name="layout" content="main" />
	<title>Edit Report</title>
</head>

<body>

	<div class="body">

		<h1>Edit Report</h1>

		<g:if test="${flash.message}">
		<div class="message">${flash.message}</div>
		</g:if>

		<g:hasErrors bean="${reportInstance}">
		<div class="errors">
			<g:renderErrors bean="${reportInstance}" as="list" />
		</div>
		</g:hasErrors>

		<g:form action="update">
			<g:hiddenField name="id" value="${reportInstance?.id}" />
			<g:hiddenField name="version" value="${reportInstance?.version}" />
			<div class="dialog">
				<table>
					<tbody>
						<tr class="prop">
							<td valign="top" class="name">Name</td>
							<td valign="top" class="value"><g:textField name="name" value="${reportInstance.name}" /></td>
						</tr>
					</tbody>
				</table>
			</div>

			<div class="buttons">
				<span class="button"><g:submitButton class="save" name='update' value="Update" /></span>
			</div>

		</g:form>
	</div>
</body>
