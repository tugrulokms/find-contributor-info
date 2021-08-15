package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public final class App {

    private static HttpURLConnection connection;
    private static int index = 0;
    private static int max = 0;
    
    public static void main(String[] args) {
        String line = "";
        String data;
        StringBuffer responseContent = new StringBuffer();
        
        JSONArray arr = new JSONArray();
        ArrayList<Integer> max_forks = new ArrayList<Integer>();
        ArrayList<String> max_repos = new ArrayList<String>();
        
        HashMap<String,Integer> fork_map = new HashMap<String,Integer>();
        HashMap<Integer,String> repo_map = new HashMap<Integer,String>();

        URL url;
        int status;
        
        // Bütün yapı bağlantıya dayalı olduğu için programı try ile başlatıyoruz
        try {

            //Bu kısım apache repo sayfalarına bakıyor, sonsuz döngüsü sayfa sayısının bilinmemesinden kaynaklı
            while(true) {
                max = 0;

                url = new URL("https://api.github.com/orgs/apache/repos?page=" + (index+1) + "&per_page=100");
                status = connect(url);

                data = readPage(status, line, responseContent);

                // Sonsuz döngüden çıkış için
                if(data.contains("{") == false)
                    break;
                   
                maxOfRepo("forks_count", "name", data, arr, repo_map, fork_map);

                responseContent = new StringBuffer();
                index++; 

            }

            Object[] temp_arr = fork_map.values().toArray();

            ArrayList<Integer> temp_list = new ArrayList<Integer>();


            for(int i = 0; i < temp_arr.length; i++)                
                temp_list.add(Integer.valueOf(temp_arr[i].toString()));


            mapAddDataRepo(5, temp_list, max_repos, max_forks, repo_map);    


            // Contribution sayfasından alınacak veriler için
            ArrayList<Integer> max_contributions = new ArrayList<Integer>();
            ArrayList<String> max_users = new ArrayList<String>();

            HashMap<String,Integer> contributions_map = new HashMap<String,Integer>();
            HashMap<Integer,String> user_map = new HashMap<Integer,String>();

            data = "";
                      
            // Her repo için contribution sayfaları
            for(int i = 0; i < max_repos.size(); i++) {

                HashMap<String,Integer> temp_contributions_map = new HashMap<String,Integer>();

                index = 0;
                max = 0;
                temp_list = new ArrayList<Integer>();
                arr = new JSONArray();        
                JSONArray jarr = new JSONArray();

                    while(true) {

                        url = new URL("https://api.github.com/repos/apache/"+ max_repos.get(i) + "/contributors?page=" + (index+1) + "&per_page=100");
                        status = connect(url);
        
                        data = readPage(status, line, responseContent);
    
                        if(data.contains("{") == false)
                            break;
                        else if (data.contains("[]"))
                            data = data.substring(2);

                        jarr = new JSONArray(data);

                        for(int j = 0; j < jarr.length(); j++) {
                            
                            JSONObject obj = jarr.getJSONObject(j);
                            arr.put(obj);

                        }

                        responseContent = new StringBuffer();  
                        index++;

                    }

                maxOfCont(max_repos.get(i), "contributions", "login", arr, user_map, temp_contributions_map, contributions_map);

                temp_arr = temp_contributions_map.values().toArray();           

                for(int k = 0; k < temp_arr.length; k++)                                
                    temp_list.add(Integer.valueOf(temp_arr[k].toString()));

                mapAddDataCont(temp_list, max_users, max_contributions, user_map);

            }

            // User sayfasından alınacak veriler için             
            ArrayList<String> companies = new ArrayList<String>();
            ArrayList<String> locations = new ArrayList<String>();

            ArrayList<String> max_users_trimmed = new ArrayList<String>();

            //User sayfası işlemleri
            for(int i = 0; i < max_users.size(); i++) {

                
                String temp_user = max_users.get(i);
                String user = temp_user.substring(temp_user.indexOf(":")+2, temp_user.length());
                
                max_users_trimmed.add(user); 

                url = new URL("https://api.github.com/users/" + max_users_trimmed.get(i));

                status = connect(url);
    
                data = readPage(status, line, responseContent);

                if (data.contains("[]"))
                    data = data.substring(2);

                JSONObject obj = new JSONObject(data);
                String company;
                String location;

                if(obj.get("company").equals(null))
                    company = "null";
                else
                    company = (String) obj.get("company");

                if(obj.get("location").equals(null))
                    location = "null";
                else
                    location = (String) obj.get("location");
                      
                companies.add(company);
                locations.add(location);

                responseContent = new StringBuffer();
            }

            File text = new File("info.txt");
            FileWriter writer = new FileWriter(text);


            for(int i = 0; i < TOTAL_USERS; i++) {

                String [] users = max_users.get(i).split(" ");
                String repo = users[0];
                String user = users[1];

                String info = "repo:" + repo + ", user:" + user + ", location:" + locations.get(i)
                + ", company:" + companies.get(i) + ", contributions:" + contributions_map.get(max_users.get(i)) + "\n";

                writer.write(info);
            }

            writer.close();
         

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (org.json.JSONException e) {
            System.err.println("API call limit exceeded");
        }
        
        connection.disconnect();
        
    }

// ------------- Buradan sonra fonksiyonlar var ----------------

    public static int connect(URL url) {

        int status = 0;

        try {  
            connection = (HttpURLConnection) url.openConnection();

            //Request setup
            connection.setRequestProperty ("Authorization", "token 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");
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
    
            status = connection.getResponseCode();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return status;

    }

    public static String readPage(int status, String line, StringBuffer responseContent) {

        try {  
            if(status > 299) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }               
                reader.close(); 
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } 
        return responseContent.toString();
    }

    public static void maxOfRepo(String str, String str2, String data, JSONArray arr, HashMap<Integer,String> h_map_string, HashMap<String,Integer> h_map_int) {
                
        arr = new JSONArray(data);
    
        String name = "";

        for(int i = 0; i < arr.length(); i++) {
            
            JSONObject obj = arr.getJSONObject(i);
            int temp = (int) obj.get(str);
            
            if(temp > max && temp != max) {
                max = temp;
                name = (String) obj.get(str2);
            }
            
        }
        h_map_int.put(name, max);
        h_map_string.put(max, name);
        
    }

    public static void maxOfCont(String repo_name, String str, String str2, JSONArray arr, HashMap<Integer,String> h_map_string, HashMap<String,Integer> temp_h_map_int, HashMap<String,Integer> h_map_int) { 

        int i_holder = 0;

        String name = "";

        for(int i = 0; i < USERS_PER_REPO; i++) {

            max = 0;

            for(int j = 0; j < arr.length(); j++) {
                
                JSONObject obj = arr.getJSONObject(j);
                int temp = (int) obj.get(str);
                
                if(temp > max && temp != max) {
                    max = temp;
                    name = repo_name + ": " + (String) obj.get(str2);
                }
                
            }
            i_holder = findIndexOfMax(arr, "contributions");

            h_map_int.put(name, max);
            temp_h_map_int.put(name, max);
            h_map_string.put(max, name);
            arr.remove(i_holder);
           
        }
        
    }   

    public static void mapAddDataRepo(int size, ArrayList<Integer>temp_list, ArrayList<String> arr_string, ArrayList<Integer> arr_int, HashMap<Integer,String> h_map) {

        for(int i = 0; i < size; i++) {

            int i_holder = findIndexOfMax(temp_list);        
            
            arr_int.add(temp_list.get(i_holder));         
            arr_string.add(h_map.get(temp_list.get(i_holder)));
            temp_list.remove(i_holder);

        }
    }

    public static void mapAddDataCont(ArrayList<Integer>temp_list, ArrayList<String> arr_string, ArrayList<Integer> arr_int, HashMap<Integer,String> h_map) {

        for(int i = 0; i < USERS_PER_REPO; i++) {
       
            arr_int.add(temp_list.get(i));
            arr_string.add(h_map.get(temp_list.get(i)));

        }
    }

    public static int findMax(ArrayList<Integer> arr_list) {


        for(int i = 0; i < arr_list.size(); i++) {                

            int temp = arr_list.get(i);     

            if(temp > max) {
                max = temp;
            }
                
        }

        return max;
    }

    public static int findIndexOfMax(ArrayList<Integer> arr_list) {
        
        int max = 0;
        int i_holder = 0;

        for(int i = 0; i < arr_list.size(); i++) {                

            int temp = arr_list.get(i);     

            if(temp > max) {
                max = temp;
                i_holder = i;
            }
                
        }

        return i_holder;
    }


    public static int findIndexOfMax(JSONArray arr, String str) {
        
        int max = 0;
        int i_holder = 0;

        for(int i = 0; i < arr.length(); i++) {

            JSONObject obj = arr.getJSONObject(i);
            int temp = (int) obj.get(str);  

            if(temp > max) {
                max = temp;
                i_holder = i;
            }
                
        }

        return i_holder;
    }

    private static final int USERS_PER_REPO = 10;
    private static final int TOTAL_USERS = 50;
    
}

