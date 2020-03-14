package mexican;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Main {

    public static void main(String[] args) {
        String inputFileName = "input-1.json";
        String outputFileName = "output.json";
        JSONArray jArray = null;

        try {
            jArray = readJSONFile(inputFileName);
        } catch(Exception e) {
            // better solution would be to write errors to a log file
            System.out.println(String.format("Could not read file. Exception message: %s", e.getMessage()));
        }

        ArrayList<Long> bestCostList = new ArrayList<Long>();
        if(jArray != null) {
            // calculate best costs for each money+menu scenario
            for(Object itemObj : jArray) {
                JSONObject item = (JSONObject) itemObj;
                bestCostList.add(calculateBestCost(item));
            }

            // write best costs to a file
            writeBestCostsToFile(outputFileName, bestCostList);
        }
    }

    private static JSONArray readJSONFile(String fileName) throws Exception {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(fileName));
        return (JSONArray) obj;
    }

    private static Long calculateBestCost(JSONObject item) {
        Long moneyVal = (Long)item.get("money");
        JSONObject mainList = (JSONObject)item.get("mains");
        JSONObject sideList = (JSONObject)item.get("sides");

        // keeping possible cost duplicates as to preserve amount of unique menu items
        ArrayList<Long> mainDishCosts = new ArrayList<Long>(mainList.values());
        ArrayList<Long> sideDishCosts = new ArrayList<Long>(sideList.values());

        // sort in descending order so I check the largest cost first
        Collections.sort(mainDishCosts, Collections.<Long>reverseOrder());
        Collections.sort(sideDishCosts, Collections.<Long>reverseOrder());

        Long bestCost = new Long(0);
        for(int mainIndex = 0; mainIndex < mainDishCosts.size(); mainIndex++) {
            // RULE: We can have up to one main dish
            Long mainCost = mainDishCosts.get(mainIndex);
            if(mainCost.compareTo(moneyVal) < 0) {
                Long currentTotalCost = mainCost;
                bestCost = Math.max(bestCost, currentTotalCost);
                // RULE: We can have 0-2 sides
                for(int i = 0; i < sideDishCosts.size(); i++) {
                    currentTotalCost += sideDishCosts.get(i);
                    if(currentTotalCost.compareTo(moneyVal) < 0) {
                        // check that we have at least one side we can possibly add
                        if(i+1 < sideDishCosts.size()) {
                            boolean foundSecondSide = false;
                            // we want to exit the first time (if) we find a second side as this will
                            // be the max cost we can get
                            for(int j=i+1; (j < sideDishCosts.size() && !foundSecondSide); j++) {
                                currentTotalCost += sideDishCosts.get(j);
                                if(currentTotalCost.compareTo(moneyVal) <= 0) {
                                    bestCost = Math.max(bestCost, currentTotalCost);
                                    foundSecondSide = true;
                                }
                                // reset for checking the next additional side dish
                                currentTotalCost -= sideDishCosts.get(j);
                            }
                        }
                        bestCost = Math.max(bestCost, currentTotalCost);
                    } else if(currentTotalCost.equals(moneyVal)) {
                        bestCost = Math.max(bestCost, currentTotalCost);
                    }
                    // reset for checking next first side dish
                    currentTotalCost = mainCost;
                }
            } else if(mainCost.equals(moneyVal)) {
                bestCost = mainCost;
                break;
            }
        }
        return bestCost;
    }

    private static void writeBestCostsToFile(String outputFileName, ArrayList<Long> list) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonObject = gson.toJson(list);
        try {
            FileWriter writer = new FileWriter(outputFileName);
            writer.write(jsonObject);
            writer.close();
        } catch(IOException e) {
            // better solution would be to write errors to a log file
            System.out.println(String.format("Could not write output to file. Exception message: %s", e.getMessage()));
        }
    }
}
