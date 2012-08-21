<head>
	<meta name="layout" content="main" />
	<title>Create Report</title>
</head>

<body>

	<div class="body">

		<h1>Create Report</h1>

		<g:if test="${flash.message}">
		<div class="message">${flash.message}</div>
		</g:if>

		<g:hasErrors bean="${reportInstance}">
		<div class="errors">
			<g:renderErrors bean="${reportInstance}" as="list" />
		</div>
		</g:hasErrors>

		<g:form action="save">
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
				<span class="button"><g:submitButton name="create" class="save" value="Create" /></span>
			</div>

		</g:form>
	</div>
</body>
