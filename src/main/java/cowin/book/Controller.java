package cowin.book;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class Controller implements ApplicationRunner {
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
    public void run(ApplicationArguments args) throws Exception {
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

        System.out.print("Enter District id/ids in comma separated format: ");
        String districtIds = sc.nextLine();
        System.out.println("-----------------------------------------------");

        String[] lineVector = districtIds.split(",");
        List<Integer> districtsValue = new ArrayList<>();
        for (int it = 0; it < lineVector.length; it++) {
            districtsValue.add(Integer.parseInt(lineVector[it]));
        }


        System.out.print("Age group required 0:for 18-44 & 1:for 45+ :");
        int ageId = sc.nextInt();
        System.out.println("-----------------------------------------------");
        int age;
        if (ageId == 0) age = 18;
        else if (ageId == 1) age = 45;
        else return;

        System.out.print("Dose required 1:for dose1 & 2:for dose2 :");
        int doseType = sc.nextInt();
        System.out.println("-----------------------------------------------");

        System.out.print("minimum vaccine for notification i.e minimum 1 required :");
        int minimum = sc.nextInt();
        System.out.println("-----------------------------------------------");

        Scanner scanner = new Scanner(System.in);

        System.out.print("search start date(7 days from start day) default:Today in format dd-MM-yyyy:");
        String startDay = scanner.nextLine();
        System.out.println("-----------------------------------------------");

        while (true) {

            for (int districtId : districtsValue) {
                String dateInString = new SimpleDateFormat(pattern).format(new Date());
                if (!startDay.equals("")) dateInString = startDay;

                String response
                        = restTemplate.getForObject(availabilityCheckURL + "district_id=" + districtId + "&date=" + dateInString, String.class);


                JSONObject obj = new JSONObject(response);
                JSONArray centers = obj.getJSONArray("centers");
                System.out.println("district name: " + centers.getJSONObject(0).getString("district_name"));
                System.out.println("total centers found: " + centers.length());
                boolean flag = false;
                for (int i = 0; i < centers.length(); i++) {
                    JSONArray sessions = centers.getJSONObject(i).getJSONArray("sessions");
                    for (int j = 0; j < sessions.length(); j++) {
                        int min_age_limit = sessions.getJSONObject(j).getInt("min_age_limit");
                        int available_capacity_dose1 = sessions.getJSONObject(j).getInt("available_capacity_dose1");
                        int available_capacity_dose2 = sessions.getJSONObject(j).getInt("available_capacity_dose2");
                        int available_capacity_dose = available_capacity_dose1;
                        if (doseType == 1) available_capacity_dose = available_capacity_dose1;
                        else if (doseType == 2) available_capacity_dose = available_capacity_dose2;
                        if (min_age_limit == age && available_capacity_dose >= minimum) {
                            System.out.println(centers.getJSONObject(i).getString("name"));
                            sound.soundPlay();
                            flag = true;

                        }

                    }

                }
                if (flag == false) System.out.println("No viable option found!");
                System.out.println("-----------------------------------------------");


            }
            TimeUnit.SECONDS.sleep(4);
        }
    }


}
