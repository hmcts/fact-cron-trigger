package uk.gov.hmcts.reform.fact.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import uk.gov.hmcts.reform.fact.exceptions.JsonConvertException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvUtil {
    public static String convertJsonToCsv(JsonNode jsonArrayNode) {
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.enable(SerializationFeature.INDENT_OUTPUT);
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();

        // Add the required columns for the CSV output
        schemaBuilder.addColumn("name")
            .addColumn("lat")
            .addColumn("lon")
            .addColumn("number")
            .addColumn("cci_code")
            .addColumn("magistrate_code")
            .addColumn("slug")
            .addColumn("types")
            .addColumn("open")
            .addColumn("dx_number")
            .addColumn("areas_of_law")  // Concatenated areas of law
            .addColumn("addresses");    // Concatenated address lines

        List<Map<String, Object>> flatList = new ArrayList<>();

        // Iterate through the JSON array (list of Court objects) to flatten the structure
        for (JsonNode node : jsonArrayNode) {
            Map<String, Object> flatMap = new LinkedHashMap<>();
            flatMap.put("name", node.path("name").asText());
            flatMap.put("lat", node.path("lat").asDouble());
            flatMap.put("lon", node.path("lon").asDouble());
            flatMap.put("number", node.path("number").asInt());
            flatMap.put("cci_code", node.path("cci_code").asInt());
            flatMap.put("magistrate_code", node.path("magistrate_code").asInt());
            flatMap.put("slug", node.path("slug").asText());
            flatMap.put("types", stringifyArray(node.path("types")));
            flatMap.put("open", node.path("displayed").asBoolean());
            flatMap.put("dx_number", node.path("dx_number").asText());

            // Areas of law
            JsonNode areasOfLawNode = node.path("areas_of_law");
            if (areasOfLawNode.isArray() && !areasOfLawNode.isEmpty()) {
                List<String> areasOfLawList = new ArrayList<>();
                for (JsonNode areaOfLaw : areasOfLawNode) {
                    String areaName = areaOfLaw.path("name").asText(null);
                    String externalLink = areaOfLaw.path("external_link").asText(null);
                    String externalLinkDesc = areaOfLaw.path("external_link_desc").asText(null);
                    String displayName = areaOfLaw.path("display_name").asText(null);
                    String displayExternalLink = areaOfLaw.path("display_external_link").asText(null);

                    String areaDetails = "Name: " + (areaName != null ? areaName : "N/A") +
                        ", External Link: " + (externalLink != null ? externalLink : "N/A") +
                        ", Description: " + (externalLinkDesc != null ? externalLinkDesc : "N/A") +
                        ", Display Name: " + (displayName != null ? displayName : "N/A") +
                        ", Display External Link: " + (displayExternalLink != null ? displayExternalLink : "N/A");

                    areasOfLawList.add(areaDetails);
                }
                flatMap.put("areas_of_law", String.join(" | ", areasOfLawList));
            } else {
                flatMap.put("areas_of_law", "No areas of law available");
            }

            // Handle 'addresses' field (list of Address objects)
            JsonNode addressesNode = node.path("addresses");
            if (addressesNode.isArray() && !addressesNode.isEmpty()) {
                List<String> addressList = new ArrayList<>();
                for (JsonNode address : addressesNode) {
                    List<String> addressLines = new ArrayList<>();
                    address.path("address_lines").forEach(line -> addressLines.add(line.asText()));
                    String postcode = address.path("postcode").asText(null);
                    String town = address.path("town").asText(null);
                    String type = address.path("type").asText(null);
                    String county = address.path("county").asText(null);
                    String description = address.path("description").asText(null);  // Added description
                    String epimId = address.path("epim_id").asText(null);  // Added epimId

                    // Handling 'fields_of_law' within the address
                    JsonNode fieldsOfLawNode = address.path("fields_of_law");
                    String fieldsOfLaw = "";
                    if (fieldsOfLawNode.isObject()) {
                        JsonNode areasOfLawInFields = fieldsOfLawNode.path("areas_of_law");
                        JsonNode courtsInFields = fieldsOfLawNode.path("courts");

                        // Handle 'areas_of_law' inside fields_of_law
                        List<String> areasOfLawInFieldsList = new ArrayList<>();
                        if (areasOfLawInFields.isArray()) {
                            areasOfLawInFields.forEach(area -> areasOfLawInFieldsList.add(area.asText()));
                        }
                        fieldsOfLaw += "Areas of Law: " + String.join(" | ", areasOfLawInFieldsList);

                        // Handle 'courts' inside fields_of_law
                        List<String> courtsInFieldsList = new ArrayList<>();
                        if (courtsInFields.isArray()) {
                            courtsInFields.forEach(court -> courtsInFieldsList.add(court.asText()));
                        }
                        if (!courtsInFieldsList.isEmpty()) {
                            fieldsOfLaw += ", Courts: " + String.join(" | ", courtsInFieldsList);
                        }
                    }

                    // Only add address if there are address lines
                    String addressDetails = "Town: " + (town != null ? town : "N/A") +
                        ", Postcode: " + (postcode != null ? postcode : "N/A") +
                        ", Address: " + (addressLines.isEmpty() ? "No address lines" : String.join(", ", addressLines)) +
                        ", Type: " + (type != null ? type : "N/A") +
                        ", County: " + (county != null ? county : "N/A") +
                        ", Fields of Law: " + (fieldsOfLaw.isEmpty() ? "" : ", " + fieldsOfLaw) +
                        ", Description: " + (description != null ? description : "N/A") +
                        ", EPIM ID: " + (epimId != null ? epimId : "N/A");

                    addressList.add(addressDetails);
                }
                flatMap.put("addresses", String.join(" | ", addressList));
            } else {
                flatMap.put("addresses", "No address available");
            }

            // Add the flattened row to the list
            flatList.add(flatMap);
        }

        // Build the final CSV schema with header
        CsvSchema schema = schemaBuilder.build().withHeader();

        try {
            return csvMapper.writer(schema).writeValueAsString(flatList);
        } catch (JsonProcessingException ex) {
            throw new JsonConvertException("Failed to convert JSON to CSV: " + ex.getMessage());
        }
    }

    private static String stringifyArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return "";
        List<String> items = new ArrayList<>();
        arrayNode.forEach(n -> items.add(n.asText()));
        return String.join(" | ", items);
    }
}





