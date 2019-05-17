package cultura;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cultura.data.Data;
import cultura.utilities.AjaxResponseBody;

@RestController
public class DataController {
	private String userEmail;

	@RequestMapping(method = RequestMethod.POST, path = "/welcome/temperature")
	public AjaxResponseBody getMeasuresTemperature(@RequestBody String userEmail) {
		this.userEmail = userEmail.replaceAll("\"", "");
		AjaxResponseBody response = new AjaxResponseBody();
		response.setMsg("response");
		response.setYAxis(new Data("select measured_value from measures m "
				+ "join measured_variables mv on mv.measured_variables_id = m.measured_variable_id "
				+ "join variables v on mv.variable_id = v.variable_id where v.variable_name = 'Temperature' "
				+ "and m.user = " + userEmail + " order by date_time asc").loadMeasures());
		response.setXAxis(new Data("select date_time from measures m "
				+ "join measured_variables mv on mv.measured_variables_id = m.measured_variable_id "
				+ "join variables v on mv.variable_id = v.variable_id where v.variable_name = 'Temperature' "
				+ "and m.user = " + userEmail + " order by date_time asc").loadDate());
		return response;
	}

	@RequestMapping(method = RequestMethod.POST, path = "/welcome/Light")
	public AjaxResponseBody getMeasuresLight(@RequestBody String userEmail) {
		this.userEmail = userEmail.replaceAll("\"", "");
		AjaxResponseBody response = new AjaxResponseBody();
		response.setMsg("response");
		response.setYAxis(new Data("select measured_value from measures m "
				+ "join measured_variables mv on mv.measured_variables_id = m.measured_variable_id "
				+ "join variables v on mv.variable_id = v.variable_id where v.variable_name = 'Light' "
				+ "and m.user = " + userEmail + " order by date_time asc").loadMeasures());
		response.setXAxis(new Data("select date_time from measures m "
				+ "join measured_variables mv on mv.measured_variables_id = m.measured_variable_id "
				+ "join variables v on mv.variable_id = v.variable_id where v.variable_name = 'Light' "
				+ "and m.user = " + userEmail + " order by date_time asc").loadDate());
		return response;
	}

	//O sensor é desnecessario
	@RequestMapping(value = "/welcome/filters", method = RequestMethod.GET)
	public AjaxResponseBody getFilteredMeasures(@RequestParam(name = "dataB") String dataB,
			@RequestParam(name = "dataF") String dataF, @RequestParam(name = "measureL") Double measureL,
			@RequestParam(name = "measureH") Double measureH, @RequestParam(name = "culture") String culture,
			@RequestParam(name = "sensor") String sensor, Model model) {

		AjaxResponseBody response = new AjaxResponseBody();
		response.setMsg("response");
		response.setYAxis(new Data("select measured_value from measures m", dataB, dataF, measureL, measureH, culture,
				sensor, this.userEmail).loadMeasures());
		response.setXAxis(new Data("select date_time from measures m", dataB, dataF, measureL, measureH, culture,
				sensor, this.userEmail).loadDate());
		return response;
	}

	@RequestMapping(value = "/addMeasure/user", method = RequestMethod.GET)
	public AjaxResponseBody getUserEmail(Model model) {
		AjaxResponseBody response = new AjaxResponseBody(this.userEmail, "response");
		return response;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
}