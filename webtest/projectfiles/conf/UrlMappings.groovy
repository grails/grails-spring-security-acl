import org.springframework.security.acls.model.NotFoundException

class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {}
		}

      "/"(view:"/index")

		"403"(controller: "errors", action: "error403")
		"404"(controller: "errors", action: "error404")
		"500"(controller: "errors", action: "error500")
		"500"(controller: "errors", action: "error403", exception: NotFoundException)
	}
}
