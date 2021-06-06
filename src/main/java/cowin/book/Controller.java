package cowin.book;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sound.sampled.LineUnavailableException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@RestController
public class Controller implements CommandLineRunner {
    @Value("${CALENDAR_URL_DISTRICT}")
    String availabilityCheckURL;
    @Value("${STATE_URL}")
    String stateURL;
    @Value("${DISTRICT_URL}")
    String districtURL;
    private String pattern = "dd-MM-yyyy";
    @Autowired
    private Sound sound;

    @Override
    public void run(String... args) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        ResponseEntity<String> stateRsponse
                = restTemplate.exchange(stateURL, HttpMethod.GET, entity, String.class);
        JSONObject stateObj = new JSONObject(stateRsponse.getBody());
        JSONArray states = stateObj.getJSONArray("states");
        for (int i = 0; i < states.length(); i++) {
            System.out.println(states.getJSONObject(i).getString("state_id") + ": " + states.getJSONObject(i).getString("state_name"));
        }
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter State id: ");
        String stateId = sc.nextLine();
        System.out.println("-----------------------------------------------");
        ResponseEntity<String> districtResponse
                = restTemplate.exchange(districtURL + stateId, HttpMethod.GET, entity, String.class);
        JSONObject districtObj = new JSONObject(districtResponse.getBody());
        JSONArray districts = districtObj.getJSONArray("districts");
        for (int i = 0; i < districts.length(); i++) {
            System.out.println(districts.getJSONObject(i).getString("district_id") + ": " + districts.getJSONObject(i).getString("district_name"));
        }

        System.out.print("Enter District id: ");
        String districtId = sc.nextLine();
        System.out.println("-----------------------------------------------");

        while (true) {

            String dateInString = new SimpleDateFormat(pattern).format(new Date());

            String response
                    = restTemplate.getForObject(availabilityCheckURL + "district_id=" + districtId + "&date=" + dateInString, String.class);

            JSONObject obj = new JSONObject(response);
            JSONArray centers = obj.getJSONArray("centers");
            System.out.println("total centers found: " + centers.length());
            boolean flag=false;
            for (int i = 0; i < centers.length(); i++) {
                JSONArray sessions = centers.getJSONObject(i).getJSONArray("sessions");
                for (int j = 0; j < sessions.length(); j++) {
                    int min_age_limit = sessions.getJSONObject(j).getInt("min_age_limit");
                    int available_capacity_dose1 = sessions.getJSONObject(j).getInt("available_capacity_dose1");
                    if (min_age_limit == 18 && available_capacity_dose1 >= 1) {
                        System.out.println(centers.getJSONObject(i).getString("name"));
                        sound.soundPlay();
                        flag=true;

                    }

                }

            }
            if(flag==false) System.out.println("No viable option found!");
            System.out.println("-----------------------------------------------");
            TimeUnit.SECONDS.sleep(3);

        }
    }
}
