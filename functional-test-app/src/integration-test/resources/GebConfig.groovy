import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

reportsDir = new File('build/geb-reports')

environments {

	// run via “./gradlew -Dgeb.env=chrome iT”
	chrome {
		driver = { new ChromeDriver() }
	}

	// run via “./gradlew -Dgeb.env=chromeHeadless iT”
	chromeHeadless {
		driver = {
			ChromeOptions o = new ChromeOptions()
			o.addArguments('headless')
			new ChromeDriver(o)
		}
	}
}
