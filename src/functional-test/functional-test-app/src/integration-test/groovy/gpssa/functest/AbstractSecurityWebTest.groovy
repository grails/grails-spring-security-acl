package gpssa.functest;

import functionaltestplugin.FunctionalTestCase

import java.util.regex.Pattern

import com.gargoylesoftware.htmlunit.HttpMethod
import com.gargoylesoftware.htmlunit.WebRequestSettings

import org.springframework.util.ReflectionUtils

abstract class AbstractSecurityWebTest extends FunctionalTestCase {

	protected static final String ROW_COUNT_XPATH = "count(//div[@class='list']//tbody/tr)"

	protected String sessionId

	@Override
	protected void tearDown() {
		super.tearDown()
		get '/logout'
	}

	protected void verifyListSize(int size) {
		assertContentContainsStrict 'List'
		int actual = page.getByXPath(ROW_COUNT_XPATH)[0]
		assertEquals "$size row(s) of data expected", size, actual
	}

	protected void clickButton(String idOrText) {
		def button = byId(idOrText)
		if (!button) {
			def form = page.forms[0]
			for (element in form.getElementsByAttribute('input', 'type', 'submit')) {
				if (element.valueAttribute == idOrText) {
					button = element
					break
				}
			}
		}

		if (!button) {
			throw new IllegalArgumentException("No such element for id or button text [$idOrText]")
		}

		println "Clicked [$idOrText] which resolved to a [${button.class}]"
		button.click()
		handleRedirects()
	}

	protected getInNewPage(String url, String sessionId = null) {
		def settings = new WebRequestSettings(makeRequestURL(page, url))
		settings.httpMethod = HttpMethod.GET
		if (sessionId) {
			settings.additionalHeaders = ['Cookie': 'JSESSIONID=' + sessionId]
		}
		dumpRequestInfo(settings)
		return client.loadWebResponse(settings)
	}

	protected String getContent(String url, boolean newPage = false) {
		def res
		if (newPage) {
			res = getInNewPage(url)
		}
		else {
			get url
			res = response
		}
		stripWS res.contentAsString
	}

	def get(url, Closure paramSetup = null) {
		super.get(url, paramSetup)
		def cookie = response.responseHeaders.find { it.name == 'Set-Cookie' }
		if (!cookie) {
			return
		}
		def parts = cookie.value.split(';Path=/')
		sessionId = parts[0] - 'JSESSIONID='
	}

	protected void login(String username, String password) {
		get '/login/auth'

		form {
			j_username = username
			j_password = password
			_spring_security_remember_me = true
			clickButton 'Login'
		}

		assertContentContains 'Welcome to Grails'
	}
}
