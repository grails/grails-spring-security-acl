import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver

reportsDir = new File('build/geb-reports')
quitCachedDriverOnShutdown = false

environments {

	htmlUnit {
		driver = { new HtmlUnitDriver() }
	}

	chrome {
		driver = { new ChromeDriver() }
	}

	firefox {
		driver = { new FirefoxDriver() }
	}

	phantomJs {
		driver = { new PhantomJSDriver() }
	}
}